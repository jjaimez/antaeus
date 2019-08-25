package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.CurrencyProvider
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

class BillingService(
        private val paymentProvider: PaymentProvider,
        private val customerService: CustomerService,
        private val invoiceService: InvoiceService,
        private val currencyProvider: CurrencyProvider
) {

    private val fetchSize = 1
    private val LOG = KotlinLogging.logger {}


    fun process(status: InvoiceStatus){
        LOG.info { "[Service:BillingService] - [Event:process] - [status:${status}]"}
        while (invoiceService.countByStatus(status) > 0){
            var invoices = invoiceService.fetchByStatus(status,fetchSize)
            for (invoice in invoices){
                var lockedInvoice = lockInovice(invoice)
                lockedInvoice?.let {
                    charge(it)
                }
            }
        }
    }

    fun lockInovice(invoice: Invoice):Invoice?{
        return invoiceService.updateStatusInvoice(invoice.id,InvoiceStatus.IN_PROCESS)
    }

    private fun charge(invoice: Invoice): Boolean{
        try {

            var varInvoice = validateInvoiceAndCorrect(invoice)
            varInvoice?.let {
                if(paymentProvider.charge(it)){
                    invoiceService.updateStatusInvoice(varInvoice.id,InvoiceStatus.PAID)
                } else {
                    invoiceService.updateStatusInvoice(varInvoice.id,InvoiceStatus.FAIL)
                }
            }

        } catch (e: Exception) {
           exeptionHandler(e, invoice)
        }
        return false;
    }

    private fun validateInvoiceAndCorrect(invoice: Invoice): Invoice? {
        val customer = customerService.fetch(invoice.customerId)

        if (invoice.amount.currency != customer.currency){
            LOG.info { "[Service:BillingService] - [Event:validateAndCorrect] - [invoiceCurrency:${invoice.amount.currency}] - [customerCurrency:${customer.currency}]"}
            val amount = currencyProvider.exchange(invoice.amount.currency, customer.currency, invoice.amount.value)
            val updatedInvoice = invoiceService.updateCurrencyAndValueInvoice(invoice.id,customer.currency, amount)
            updatedInvoice.let { return updatedInvoice }
        }

        return invoice
    }

    fun exeptionHandler(e: Exception, invoice: Invoice){
        when (e) {
            is CustomerNotFoundException -> customerNotFoundExceptionHandler(invoice)
            is NetworkException -> NetworkExceptionHandler(invoice)
            is CurrencyMismatchException -> currencyMismatchExceptionHandler(invoice)
            else -> genericExceptionHandler(e, invoice)
        }
    }


    fun customerNotFoundExceptionHandler(invoice: Invoice){
        LOG.error { "[Service:BillingService] - [Error:customerNotFoundExceptionHandler] - [id:${invoice.id}] - [customer:${invoice.customerId}]"}
        invoiceService.updateStatusInvoice(invoice.id,InvoiceStatus.INCONSISTENT)
    }

    fun NetworkExceptionHandler(invoice: Invoice){
        LOG.error { "[Service:BillingService] - [Error:NetworkExceptionHandler] - [id:${invoice.id}] - [customer:${invoice.customerId}]"}
        invoiceService.updateStatusInvoice(invoice.id,InvoiceStatus.FAIL)
    }

    fun currencyMismatchExceptionHandler(invoice: Invoice){
        LOG.error { "[Service:BillingService] - [Error:currencyMismatchExceptionHandler] - [id:${invoice.id}] - [customer:${invoice.customerId}]"}
        invoiceService.updateStatusInvoice(invoice.id,InvoiceStatus.FAIL)
    }

    fun genericExceptionHandler(e: Exception, invoice: Invoice){
        LOG.error { "[Service:BillingService] - [Error:genericExceptionHandler] - [id:${invoice.id}] - [customer:${invoice.customerId}] - [Message::${e.message}]"}
        invoiceService.updateStatusInvoice(invoice.id,InvoiceStatus.FAIL)
    }


}