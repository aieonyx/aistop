// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.core

/**
 * ExemptDatabase — apps that AI Stop never monitors.
 *
 * Banking and financial apps use fraud detection that conflicts
 * with Android Accessibility Services. AI Stop respects their
 * security model and exempts them completely.
 *
 * These apps are NOT AI apps and pose no AI data risk to users.
 * GCash is used by 94M+ Filipinos including millions abroad.
 */
object ExemptDatabase {

    val EXEMPT_PACKAGES: Set<String> = setOf(

        // ── Philippines ────────────────────────────────────────────────────
        "com.globe.gcash.android",           // GCash — #1 PH mobile wallet (94M users)
        "ph.com.globe.globeonesuperapp",     // Globe One
        "com.maya.app",                      // Maya (PayMaya)
        "com.coins.ph",                      // Coins.ph
        "com.bdo.mobile",                    // BDO Unibank
        "com.unionbankph.retail",            // UnionBank Online
        "com.metrobankdirect.mobile",        // Metrobank Mobile
        "com.landbank.mobile",               // Landbank Mobile
        "com.rcbc.bankard",                  // RCBC DiskarTech
        "ph.pnb.mobile",                     // PNB Mobile
        "com.securitybank.mobile",           // Security Bank
        "com.eastwestbanker.mobile",         // EastWest Bank
        "ph.com.bpi.bpiapp",                 // BPI Mobile

        // ── Czech Republic ────────────────────────────────────────────────
        "cz.csas.smartbanking",              // CSAS George
        "cz.kb.smartbanking",                // Komercni Banka
        "cz.airbank.personal.android",       // Air Bank
        "cz.csob.smartkey",                  // CSOB SmartKey
        "cz.moneta.smartbanking",            // Moneta Money Bank
        "cz.rb.smartbanking",                // Raiffeisenbank CZ
        "cz.fio.mobile",                     // Fio Banka
        "cz.unicreditbank.smartbanking",     // UniCredit CZ

        // ── Germany ───────────────────────────────────────────────────────
        "de.number26.android",               // N26
        "com.commerzbank.mobile",            // Commerzbank
        "de.comdirect.android",              // Comdirect
        "de.deutschebank.mobilebanking",     // Deutsche Bank
        "de.santander.presentation",         // Santander DE
        "de.postbank.finanzassistent",       // Postbank
        "de.ingdiba.banking",                // ING-DiBa
        "com.traderepublic.app",             // Trade Republic
        "de.sparkasse.banking",              // Sparkasse

        // ── France ────────────────────────────────────────────────────────
        "fr.lcl.android.customerarea",       // LCL Mes Comptes
        "com.bnpparibas.mescomptes",         // BNP Paribas
        "fr.creditagricole.androidapp",      // Crédit Agricole
        "com.societegenerale.mobile",        // Société Générale
        "fr.labanquepostale.android",        // La Banque Postale
        "com.boursorama.android",            // Boursorama
        "fr.fortuneo.android",               // Fortuneo

        // ── Australia ─────────────────────────────────────────────────────
        "com.anz.android",                   // ANZ Mobile Banking
        "au.com.nab.mobile",                 // NAB Mobile Banking
        "au.com.commbank.commbankapp",       // CommBank
        "com.westpac.mobile",                // Westpac Mobile
        "au.com.bendigobank.app",            // Bendigo Bank
        "au.com.macquarie.banking",          // Macquarie Mobile
        "com.ingdirect.android",             // ING Australia
        "au.com.ubank.app",                  // UBank
        "com.up.bank",                       // Up Banking

        // ── Japan ─────────────────────────────────────────────────────────
        "jp.co.smbc.smbc_direct",            // SMBC Direct
        "jp.co.mizuhobank.mizuhobankapp",    // Mizuho Bank
        "jp.mufg.bk.applisp.app",            // MUFG (Mitsubishi UFJ)
        "jp.co.rakuten_bank.android",        // Rakuten Bank
        "jp.co.sony.financenet.sonybank",    // Sony Bank

        // ── UK ────────────────────────────────────────────────────────────
        "com.barclays.android.barclaysmobilebankingapp", // Barclays
        "com.tescobank.mobile",              // Tesco Bank
        "com.lloydsbank.mobile",             // Lloyds Bank
        "com.hsbc.hsbcukmobilebanking",      // HSBC UK
        "com.monzo.money",                   // Monzo
        "com.starlingbank.banking",          // Starling Bank

        // ── Global / International ────────────────────────────────────────
        "com.revolut.revolut",               // Revolut (70M+ users globally)
        "com.wise.pwa",                      // Wise
        "com.transferwise.android",          // Wise legacy
        "com.paypal.android.p2pmobile",      // PayPal
        "com.google.android.apps.walletnfcrel", // Google Wallet
        "com.samsung.android.spay",          // Samsung Pay
        "com.apple.android.wallet",          // Apple Wallet Android
        "com.n26.android",                   // N26 international
        "com.klarna.app",                    // Klarna
        "com.stripe.android",                // Stripe
    )

    /**
     * Returns true if the package should be completely exempt
     * from AI Stop monitoring.
     */
    fun isExempt(packageName: String): Boolean =
        packageName in EXEMPT_PACKAGES

    /**
     * User-visible label for exempt status.
     */
    fun exemptReason(packageName: String): String =
        "Banking/financial app — AI Stop never monitors financial apps for your security"
}
