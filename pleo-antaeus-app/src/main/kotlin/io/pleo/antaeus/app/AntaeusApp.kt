/*
    Defines the main() entry point of the app.
    Configures the database and sets up the REST web service.
 */

@file:JvmName("AntaeusApp")

package io.pleo.antaeus.app

import getCurrencyProvider
import getPaymentProvider
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.utils.TimeService
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.rest.AntaeusRest
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import setupInitialData
import java.sql.Connection

private val LOG = KotlinLogging.logger {}

fun main() {
    // The tables to create in the database.
    val tables = arrayOf(InvoiceTable, CustomerTable)

    // Connect to the database and create the needed tables. Drop any existing data.
    val db = Database
        .connect("jdbc:sqlite:/tmp/data.db", "org.sqlite.JDBC")
        .also {
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            transaction(it) {
                addLogger(StdOutSqlLogger)
                // Drop all existing tables to ensure a clean slate on each run
                SchemaUtils.drop(*tables)
                // Create all tables
                SchemaUtils.create(*tables)
            }
        }
    // Set up data access layer.
    val dal = AntaeusDal(db = db)

    // Insert example data in the database.
    setupInitialData(dal = dal)

    // Get third parties
    val paymentProvider = getPaymentProvider()
    val currencyProvider = getCurrencyProvider()

    // Create core services
    val invoiceService = InvoiceService(dal = dal)
    val customerService = CustomerService(dal = dal)
    val timeService = TimeService("Europe/Copenhagen")


    // This is _your_ billing service to be included where you see fit
    val billingService = BillingService(
            paymentProvider = paymentProvider,
            customerService = customerService,
            invoiceService = invoiceService,
            currencyProvider =  currencyProvider
    )

    GlobalScope.async {
        while(true) {
            if (timeService.isFirst()) {
                LOG.info { LOG.info { "[Service:Main] - [Event:billingServiceStart]"} }
                billingService.process(InvoiceStatus.PENDING)
                billingService.process(InvoiceStatus.FAIL)
            }
            var distanceToFirstOfNextMonthDistance = timeService.getDistanceToFirstOfNextMonth()
            LOG.info { "[Service:Main] - [Event:billingServiceSleepFor ${distanceToFirstOfNextMonthDistance}]"}
            delay(distanceToFirstOfNextMonthDistance)
            LOG.info { "[Service:Main] - [Event:billingServiceWekingUpAfter ${distanceToFirstOfNextMonthDistance}]"}
        }
    }

    // Create REST web service
    AntaeusRest(
        invoiceService = invoiceService,
        customerService = customerService
    ).run()
}

