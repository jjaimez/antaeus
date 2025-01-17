/*
    Implements the data access layer (DAL).
    This file implements the database queries used to fetch and insert rows in our database tables.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal

class AntaeusDal(private val db: Database) {
    fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                .select { InvoiceTable.id.eq(id) }
                .firstOrNull()
                ?.toInvoice()
        }
    }

    fun fetchInvoices(): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                .selectAll()
                .map { it.toInvoice() }
        }
    }


    fun fetchInvoicesByStatus(status: String): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                    .select({InvoiceTable.status.eq(status)})
                    .map { it.toInvoice() }
        }
    }

    fun fetchInvoicesByStatus(status: String, size: Int): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                    .select({InvoiceTable.status.eq(status)})
                    .fetchSize(size)
                    .map { it.toInvoice() }
        }
    }


    fun countInvoicesByStatus(status: String): Int {
        return transaction(db) {
            InvoiceTable
                    .select({InvoiceTable.status.eq(status)})
                    .count()
        }
    }


    fun updateStatusInvoice(id: Int, status: String): Invoice? {
        transaction(db) {
            InvoiceTable
                    .update({InvoiceTable.id.eq(id)}) {
                        it[this.status] = status
                    }

        }

        return fetchInvoice(id)
    }

    fun updateCurrencyAndValueInvoice(id: Int, currency: String, value: BigDecimal): Invoice? {
        transaction(db) {
            InvoiceTable
                    .update({InvoiceTable.id.eq(id)}) {
                        it[this.value] = value
                        it[this.currency] = currency
                    }

        }
        return fetchInvoice(id)
    }

    fun createInvoice(amount: Money, customer: Customer, status: InvoiceStatus = InvoiceStatus.PENDING): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                .insert {
                    it[this.value] = amount.value
                    it[this.currency] = amount.currency.toString()
                    it[this.status] = status.toString()
                    it[this.customerId] = customer.id
                } get InvoiceTable.id
        }

        return fetchInvoice(id!!)
    }

    fun fetchCustomer(id: Int): Customer? {
        return transaction(db) {
            CustomerTable
                .select { CustomerTable.id.eq(id) }
                .firstOrNull()
                ?.toCustomer()
        }
    }

    fun fetchCustomers(): List<Customer> {
        return transaction(db) {
            CustomerTable
                .selectAll()
                .map { it.toCustomer() }
        }
    }

    fun createCustomer(currency: Currency): Customer? {
        val id = transaction(db) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
            } get CustomerTable.id
        }

        return fetchCustomer(id!!)
    }
}
