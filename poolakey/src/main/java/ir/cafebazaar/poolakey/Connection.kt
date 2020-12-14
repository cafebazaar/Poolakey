package ir.cafebazaar.poolakey

interface Connection {

    /**
     * You can use this function to get notified about the billing service's connection state.
     * @return ConnectionState which represents the current state of the billing service.
     * @see ConnectionState
     */
    fun getState(): ConnectionState

    /**
     * You can use this function to actually disconnect from the billing service.
     */
    fun disconnect()

}
