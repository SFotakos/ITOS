package io.github.sfotakos.itos.network

class ResponseWrapper<out T>(val data: T?, val apiException: ApiException?)