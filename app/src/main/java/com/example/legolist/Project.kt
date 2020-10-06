package com.example.legolist

class Project {
    var id: Int = 0
    var name: String? = null
    var active: Int = 1
    var lastAccessed: Int = 0

    constructor(id: Int, name: String) {
        this.id = id
        this.name = name
    }

    constructor(id: Int, name: String, active: Int, lastAccessed: Int) {
        this.id = id
        this.name = name
        this.active = active
        this.lastAccessed = lastAccessed
    }

}
