package io.pleo.antaeus.core.services

import io.mockk.*
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.CurrencyProvider
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal


class BillingServiceTest {
    private val customerService = mockk<CustomerService>()
    private val invoiceService = mockk<InvoiceService>()
    private val paymentProvider = mockk<PaymentProvider>()
    private val currencyProvider = mockk<CurrencyProvider>()

    private  val billingService = BillingService(
            paymentProvider = paymentProvider,
            customerService = customerService,
            invoiceService = invoiceService,
            currencyProvider =  currencyProvider
    )

    val failinvoice = getInvoice(InvoiceStatus.FAIL)

    val InProcessInvoice = getInvoice(InvoiceStatus.IN_PROCESS)

    val paidInvoice = getInvoice(InvoiceStatus.PAID)

    val InconsistentInvoice = getInvoice(InvoiceStatus.INCONSISTENT)

    val invoice = getInvoice(InvoiceStatus.PENDING)

    val failInvoice = getInvoice(InvoiceStatus.FAIL)


    //PENDING TEST

    @Test
    fun `will mark invoice as INCONSISTENT when customer is not found`() {

        every { invoiceService.countByStatus(any())} returns 1 andThen 0
        every { invoiceService.fetchByStatus(InvoiceStatus.PENDING,1)} returns listOf(invoice)
        every { customerService.fetch(1)} throws CustomerNotFoundException(1)
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)} returns InProcessInvoice
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.INCONSISTENT)} returns InconsistentInvoice

        billingService.process(InvoiceStatus.PENDING)

        verifySequence(){
            invoiceService.countByStatus(InvoiceStatus.PENDING)
            invoiceService.fetchByStatus(InvoiceStatus.PENDING,1)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)
            customerService.fetch(1)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.INCONSISTENT)
            invoiceService.countByStatus(InvoiceStatus.PENDING)
        }


    }

    @Test
    fun `will mark invoice as FAIL when NetworkException`() {

        every { invoiceService.countByStatus(any())} returns 1 andThen 0
        every { invoiceService.fetchByStatus(InvoiceStatus.PENDING,1)} returns listOf(invoice)
        every { customerService.fetch(1)} returns getCustomer(Currency.EUR)
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)} returns InProcessInvoice
        every { paymentProvider.charge(InProcessInvoice) } throws NetworkException()
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.FAIL)} returns failInvoice



        billingService.process(InvoiceStatus.PENDING)


        verifySequence(){
            invoiceService.countByStatus(InvoiceStatus.PENDING)
            invoiceService.fetchByStatus(InvoiceStatus.PENDING,1)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)
            customerService.fetch(1)
            paymentProvider.charge(InProcessInvoice)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.FAIL)
            invoiceService.countByStatus(InvoiceStatus.PENDING)
        }

    }


    @Test
    fun `will mark invoice as FAIL when CurrencyMismatchException`() {

        every { invoiceService.countByStatus(any())} returns 1 andThen 0
        every { invoiceService.fetchByStatus(InvoiceStatus.PENDING,1)} returns listOf(invoice)
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)} returns InProcessInvoice
        every { customerService.fetch(1)} returns getCustomer(Currency.EUR)
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.FAIL)} returns failInvoice
        every { paymentProvider.charge(InProcessInvoice) } throws CurrencyMismatchException(1, 1)

        billingService.process(InvoiceStatus.PENDING)


        verifySequence(){
            invoiceService.countByStatus(InvoiceStatus.PENDING)
            invoiceService.fetchByStatus(InvoiceStatus.PENDING,1)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)
            customerService.fetch(1)
            paymentProvider.charge(InProcessInvoice)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.FAIL)
            invoiceService.countByStatus(InvoiceStatus.PENDING)
        }


    }


    @Test
    fun `will mark invoice as FAIL when PaymentProvider return false`() {


        every { invoiceService.countByStatus(any())} returns 1 andThen 0
        every { invoiceService.fetchByStatus(InvoiceStatus.PENDING,1)} returns listOf(invoice)
        every { customerService.fetch(1)} returns getCustomer(Currency.EUR)
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)} returns InProcessInvoice
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.FAIL)} returns failInvoice
        every { paymentProvider.charge(InProcessInvoice) } returns false

        billingService.process(InvoiceStatus.PENDING)


        verifySequence(){
            invoiceService.countByStatus(InvoiceStatus.PENDING)
            invoiceService.fetchByStatus(InvoiceStatus.PENDING,1)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)
            customerService.fetch(1)
            paymentProvider.charge(InProcessInvoice)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.FAIL)
            invoiceService.countByStatus(InvoiceStatus.PENDING)
        }


    }

    @Test
    fun `will mark invoice as PAID when PaymentProvider return true`() {

        every { invoiceService.countByStatus(any())} returns 1 andThen 0
        every { invoiceService.fetchByStatus(InvoiceStatus.PENDING,1)} returns listOf(invoice)
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)} returns InProcessInvoice
        every { customerService.fetch(1)} returns getCustomer(Currency.EUR)
        every { paymentProvider.charge(InProcessInvoice) } returns true
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.PAID)} returns paidInvoice

        billingService.process(InvoiceStatus.PENDING)


        verifySequence(){
            invoiceService.countByStatus(InvoiceStatus.PENDING)
            invoiceService.fetchByStatus(InvoiceStatus.PENDING,1)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)
            customerService.fetch(1)
            paymentProvider.charge(InProcessInvoice)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.PAID)
            invoiceService.countByStatus(InvoiceStatus.PENDING)
        }


    }


    @Test
    fun `will mark 2 invoices as PAID when PaymentProvider return true`() {

        every { invoiceService.countByStatus(any())} returns 1 andThen 1 andThen 0
        every { invoiceService.fetchByStatus(InvoiceStatus.PENDING,1)} returns listOf(invoice)
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)} returns InProcessInvoice
        every { customerService.fetch(1)} returns getCustomer(Currency.EUR)
        every { paymentProvider.charge(InProcessInvoice) } returns true
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.PAID)} returns paidInvoice

        billingService.process(InvoiceStatus.PENDING)


        verifySequence(){
            invoiceService.countByStatus(InvoiceStatus.PENDING)
            invoiceService.fetchByStatus(InvoiceStatus.PENDING,1)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)
            customerService.fetch(1)
            paymentProvider.charge(InProcessInvoice)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.PAID)
            invoiceService.countByStatus(InvoiceStatus.PENDING)
            invoiceService.fetchByStatus(InvoiceStatus.PENDING,1)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)
            customerService.fetch(1)
            paymentProvider.charge(InProcessInvoice)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.PAID)
            invoiceService.countByStatus(InvoiceStatus.PENDING)
        }

    }

    @Test
    fun `will mark one invoice as PAID and the other as Fail when PaymentProvider return true and false`() {

        every { invoiceService.countByStatus(any())} returns 1 andThen 1 andThen 0
        every { invoiceService.fetchByStatus(InvoiceStatus.PENDING,1)} returns listOf(invoice)
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)} returns InProcessInvoice
        every { customerService.fetch(1)} returns getCustomer(Currency.EUR)
        every { paymentProvider.charge(InProcessInvoice) } returns true andThen false
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.PAID)} returns paidInvoice
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.FAIL)} returns failInvoice



        billingService.process(InvoiceStatus.PENDING)


        verifySequence(){
            invoiceService.countByStatus(InvoiceStatus.PENDING)
            invoiceService.fetchByStatus(InvoiceStatus.PENDING,1)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)
            customerService.fetch(1)
            paymentProvider.charge(InProcessInvoice)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.PAID)
            invoiceService.countByStatus(InvoiceStatus.PENDING)
            invoiceService.fetchByStatus(InvoiceStatus.PENDING,1)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)
            customerService.fetch(1)
            paymentProvider.charge(InProcessInvoice)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.FAIL)
            invoiceService.countByStatus(InvoiceStatus.PENDING)
        }

    }


    @Test
    fun `will change currency when inovice currency is different than customer currency`() {

        val invoiceCurrencyExchange = Invoice(
                id = 1,
                customerId = 1,
                amount = Money(
                        value=  BigDecimal(11),
                        currency = Currency.EUR
                ),
                status = InvoiceStatus.IN_PROCESS
        )

        val paidInvoice = Invoice(
                id = 1,
                customerId = 1,
                amount = Money(
                        value=  BigDecimal(11),
                        currency = Currency.EUR
                ),
                status = InvoiceStatus.PAID
        )

        every { invoiceService.countByStatus(any())} returns 1 andThen 0
        every { invoiceService.fetchByStatus(InvoiceStatus.PENDING,1)} returns listOf(invoice)
        every { customerService.fetch(1)} returns  getCustomer(Currency.USD)
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)} returns InProcessInvoice
        every { paymentProvider.charge(invoiceCurrencyExchange) } returns true
        every { currencyProvider.exchange(Currency.EUR, Currency.USD, BigDecimal.TEN) } returns BigDecimal(11)
        every { invoiceService.updateCurrencyAndValueInvoice(1,Currency.USD, BigDecimal(11)) } returns invoiceCurrencyExchange
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.PAID)} returns paidInvoice



        billingService.process(InvoiceStatus.PENDING)


        verifySequence(){
            invoiceService.countByStatus(InvoiceStatus.PENDING)
            invoiceService.fetchByStatus(InvoiceStatus.PENDING,1)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)
            customerService.fetch(1)
            currencyProvider.exchange(Currency.EUR, Currency.USD, BigDecimal.TEN)
            invoiceService.updateCurrencyAndValueInvoice(1,Currency.USD, BigDecimal(11))
            paymentProvider.charge(invoiceCurrencyExchange)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.PAID)
            invoiceService.countByStatus(InvoiceStatus.PENDING)
        }
    }


    //FAIL TEST
    @Test
    fun `will mark fail invoice as FAIL when NetworkException`() {

        every { invoiceService.countByStatus(any())} returns 1 andThen 0
        every { invoiceService.fetchByStatus(InvoiceStatus.FAIL,1)} returns listOf(failinvoice)
        every { customerService.fetch(1)} returns getCustomer(Currency.EUR)
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)} returns InProcessInvoice
        every { paymentProvider.charge(InProcessInvoice) } throws NetworkException()
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.FAIL)} returns failInvoice



        billingService.process(InvoiceStatus.FAIL)


        verifySequence(){
            invoiceService.countByStatus(InvoiceStatus.FAIL)
            invoiceService.fetchByStatus(InvoiceStatus.FAIL,1)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)
            customerService.fetch(1)
            paymentProvider.charge(InProcessInvoice)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.FAIL)
            invoiceService.countByStatus(InvoiceStatus.FAIL)
        }

    }


    @Test
    fun `will mark fail invoice as FAIL when CurrencyMismatchException`() {

        every { invoiceService.countByStatus(any())} returns 1 andThen 0
        every { invoiceService.fetchByStatus(InvoiceStatus.FAIL,1)} returns listOf(failinvoice)
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)} returns InProcessInvoice
        every { customerService.fetch(1)} returns getCustomer(Currency.EUR)
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.FAIL)} returns failInvoice
        every { paymentProvider.charge(InProcessInvoice) } throws CurrencyMismatchException(1, 1)

        billingService.process(InvoiceStatus.FAIL)


        verifySequence(){
            invoiceService.countByStatus(InvoiceStatus.FAIL)
            invoiceService.fetchByStatus(InvoiceStatus.FAIL,1)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)
            customerService.fetch(1)
            paymentProvider.charge(InProcessInvoice)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.FAIL)
            invoiceService.countByStatus(InvoiceStatus.FAIL)
        }


    }


    @Test
    fun `will mark fail invoice as FAIL when PaymentProvider return false`() {

        every { invoiceService.countByStatus(any())} returns 1 andThen 0
        every { invoiceService.fetchByStatus(InvoiceStatus.FAIL,1)} returns listOf(failinvoice)
        every { customerService.fetch(1)} returns getCustomer(Currency.EUR)
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)} returns InProcessInvoice
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.FAIL)} returns failInvoice
        every { paymentProvider.charge(InProcessInvoice) } returns false

        billingService.process(InvoiceStatus.FAIL)


        verifySequence(){
            invoiceService.countByStatus(InvoiceStatus.FAIL)
            invoiceService.fetchByStatus(InvoiceStatus.FAIL,1)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)
            customerService.fetch(1)
            paymentProvider.charge(InProcessInvoice)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.FAIL)
            invoiceService.countByStatus(InvoiceStatus.FAIL)
        }


    }

    @Test
    fun `will mark fail invoice as PAID when PaymentProvider return true`() {

        every { invoiceService.countByStatus(any())} returns 1 andThen 0
        every { invoiceService.fetchByStatus(InvoiceStatus.FAIL,1)} returns listOf(failinvoice)
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)} returns InProcessInvoice
        every { customerService.fetch(1)} returns getCustomer(Currency.EUR)
        every { paymentProvider.charge(InProcessInvoice) } returns true
        every { invoiceService.updateStatusInvoice(1,InvoiceStatus.PAID)} returns paidInvoice

        billingService.process(InvoiceStatus.FAIL)


        verifySequence(){
            invoiceService.countByStatus(InvoiceStatus.FAIL)
            invoiceService.fetchByStatus(InvoiceStatus.FAIL,1)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.IN_PROCESS)
            customerService.fetch(1)
            paymentProvider.charge(InProcessInvoice)
            invoiceService.updateStatusInvoice(1,InvoiceStatus.PAID)
            invoiceService.countByStatus(InvoiceStatus.FAIL)
        }


    }

    //helpers
    private fun getInvoice(status: InvoiceStatus): Invoice {
        val invoice = Invoice(
                id = 1,
                customerId = 1,
                amount = Money(
                        value=  BigDecimal.TEN,
                        currency = Currency.EUR
                ),
                status = status
        )
        return invoice
    }

    private fun getCustomer(currency: Currency): Customer {
        val customer = Customer(
                id = 1,
                currency = currency
        )
        return customer
    }


}