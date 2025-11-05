package com.example.croachcombat.network

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "Valute", strict = false)
data class Valute @JvmOverloads constructor(
    @field:Element(name = "NumCode")
    var numCode: String = "",

    @field:Element(name = "CharCode")
    var charCode: String = "",

    @field:Element(name = "Nominal")
    var nominal: Int = 0,

    @field:Element(name = "Name")
    var name: String = "",

    @field:Element(name = "Value")
    var value: String = ""
)

@Root(name = "ValCurs", strict = false)
data class ValCurs @JvmOverloads constructor(
    @field:Element(name = "Date")
    var date: String = "",

    @field:Element(name = "name")
    var name: String = "",

    @field:ElementList(inline = true, required = false)
    var valutes: List<Valute> = emptyList()
)
