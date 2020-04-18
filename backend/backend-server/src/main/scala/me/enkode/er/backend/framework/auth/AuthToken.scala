package me.enkode.er.backend.framework.auth

case class AuthToken(header: String, body: String, signature: String)
