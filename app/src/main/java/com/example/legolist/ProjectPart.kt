package com.example.legolist

class ProjectPart {
    var id: Int = 0
    var inventoryID: Int = 0
    var typeID: String? = null
    var itemID: String? = null
    var quantityInSet: Int = 0
    var quantityInStore: Int = 0
    var colorID: String? = null
    var extra: String? = null

    constructor(
        id: Int, inventoryID: Int, typeID: String, itemID: String, quantityInSet: Int,
        quantityInStore: Int, colorID: String, extra: String
    ) {
        this.id = id
        this.inventoryID = inventoryID
        this.typeID = typeID
        this.itemID = itemID
        this.quantityInSet = quantityInSet
        this.quantityInStore = quantityInStore
        this.colorID = colorID
        this.extra = extra
    }
    constructor()
}