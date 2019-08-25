package io.pleo.antaeus.core.external

import java.math.BigDecimal

import io.pleo.antaeus.models.Currency


interface CurrencyProvider {

    /*
        convert amount from one currency to another
    */
    fun exchange(sourceCurrency: Currency, destinationCurrency: Currency, amount: BigDecimal): BigDecimal

}