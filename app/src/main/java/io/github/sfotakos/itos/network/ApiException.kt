package io.github.sfotakos.itos.network

data class ApiException(val statusCode: Int, val message: String, val key: String) {

    var errorMessageTemplate = "statusCode: %d \nmessage: %s"

    fun getErrorMessage() : String{
        return errorMessageTemplate.format(statusCode, message)
    }
}