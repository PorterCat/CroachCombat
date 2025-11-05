package com.example.croachcombat.network

import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "Record", strict = false)
data class MetalRecord @JvmOverloads constructor(
    @field:Attribute(name = "Date", required = false)
    var date: String = "",

    @field:Attribute(name = "Code", required = false)
    var code: String = "",

    @field:Element(name = "Buy", required = false)
    var buy: String = "",

    @field:Element(name = "Sell", required = false)
    var sell: String = ""
)

@Root(name = "Metall", strict = false)
data class MetalRates @JvmOverloads constructor(
    @field:Attribute(name = "FromDate", required = false)
    var fromDate: String = "",

    @field:Attribute(name = "ToDate", required = false)
    var toDate: String = "",

    @field:Attribute(name = "name", required = false)
    var name: String = "",

    @field:ElementList(inline = true, required = false)
    var records: MutableList<MetalRecord> = ArrayList()
)
