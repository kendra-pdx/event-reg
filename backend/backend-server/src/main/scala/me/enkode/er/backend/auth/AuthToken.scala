package me.enkode.er.backend.auth

case class AuthToken(header: String, body: String, signature: String)
