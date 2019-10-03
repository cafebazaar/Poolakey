package com.phelat.poolakey

interface Connection {

    fun getState(): ConnectionState

    fun disconnect()

}
