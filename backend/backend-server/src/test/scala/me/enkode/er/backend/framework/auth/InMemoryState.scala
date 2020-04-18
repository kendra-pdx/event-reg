package me.enkode.er.backend.framework.auth

case class InMemoryState(
  keys: Map[KeyId, Key] = Map.empty
)
