/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Currency
import mu.KotlinLogging
import java.math.BigDecimal


class InvoiceService(private val dal: AntaeusDal) {
    private val LOG = KotlinLogging.logger {}

    fun fetchAll(): List<Invoice> {
       return dal.fetchInvoices()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun updateStatusInvoice(id: Int, status: InvoiceStatus): Invoice? {
        LOG.info { "[Service:InvoiceService] - [Event:updateStatusInvoice] - [id:${id}] - [status:${status}]"}
        return dal.updateStatusInvoice(id, status.toString())

    }

    fun updateCurrencyAndValueInvoice(id: Int, currency: Currency, value: BigDecimal): Invoice? {
        LOG.info { "[Service:InvoiceService] - [Event:updateStatusInvoice] - [id:${id}] - [currency:${currency}] - [BigDecimal:${value}]"}
        return dal.updateCurrencyAndValueInvoice(id, currency.toString(),value)
    }


    fun fetchByStatus(status: InvoiceStatus, size: Int): List<Invoice>  {
        return dal.fetchInvoicesByStatus(status.toString(), size)
    }

    fun countByStatus(status: InvoiceStatus): Int {
        return dal.countInvoicesByStatus(status.toString())
    }


}
