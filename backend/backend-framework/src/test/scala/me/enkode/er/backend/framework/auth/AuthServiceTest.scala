package me.enkode.er.backend.framework.auth

import java.time.temporal.ChronoUnit
import java.util.Base64

import cats.data._
import cats.implicits._
import cats.mtl.implicits._
import me.enkode.test_utils.implicits._
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.must.Matchers

class AuthServiceTest extends AnyFeatureSpec with Matchers with Data {
  type TestState[A] = StateT[Either[Throwable, *], InMemoryState, A]

  val keyRepository = new InMemoryKeyRepository[TestState[*]]
  val authService = new AuthService[TestState[*]](keyRepository)

  Feature("encoding auth info into a token") {
    Scenario("should be able to create a token from info") {
      val initialState = InMemoryState(
        keys = Map(keyIdA -> keyA)
      )

      val authToken = authService.authInfoToToken(validAuthInfo).runA(initialState).valueOr(throw _)

      val bePositive = be > 0
      authToken.header.length must bePositive
      authToken.body.length must bePositive
    }

    Scenario("should be able to render and parse into the same result") {
      val initialState = InMemoryState(
        keys = Map(keyIdA -> keyA)
      )

      val (rendered, parsed, original) = (for {
        authToken <- authService.authInfoToToken(validAuthInfo)
      } yield {
        val rendered = authService.renderToken(authToken)

        val parsed = authService.parseJwt(rendered)
        (rendered, parsed, authToken)
      }).runA(initialState).valueOr(throw _)

      val bePositive = be > 0
      rendered.length must bePositive
      parsed must be(original)
    }
  }

  Feature("validating auth tokens") {
    Scenario("validating a perfectly good auth token") {
      val initialState = InMemoryState(
        keys = Map(keyIdA -> keyA)
      )

      val authInfo = (for {
        authToken <- authService.authInfoToToken(validAuthInfo)
        result <- authService.validateAuthToken(authToken)
      } yield {
        result
      }).runA(initialState).valueOr(throw _)

      authInfo must contain value validAuthInfo
    }

    Scenario("validating a token with a malformed issuer should fail") {
      pending
    }

    Scenario("validating a good token but an expired key should fail") {
      val initialState = InMemoryState(
        keys = Map(keyIdA -> keyA.copy(
          expires = now().minus(10, ChronoUnit.DAYS),
        ))
      )

      val authInfo = (for {
        authToken <- authService.authInfoToToken(validAuthInfo)
        result <- authService.validateAuthToken(authToken)
      } yield {
        result
      }).runA(initialState).valueOr(throw _)

      authInfo must contain value AuthService.KeyOutOfDate
    }

    Scenario("validating a good token but a key that should not be used yet") {
      val initialState = InMemoryState(
        keys = Map(keyIdA -> keyA.copy(
          notBefore = now().plus(10, ChronoUnit.DAYS),
        ))
      )

      val authInfo = (for {
        authToken <- authService.authInfoToToken(validAuthInfo)
        result <- authService.validateAuthToken(authToken)
      } yield {
        result
      }).runA(initialState).valueOr(throw _)

      authInfo must contain value AuthService.KeyOutOfDate
    }

    Scenario("validating a token that's missing fields should fail") {
      def tamper(token: AuthToken): AuthToken = {
        token.copy(body = Base64.getEncoder.encodeToString("{}".getBytes()))
      }

      val initialState = InMemoryState(
        keys = Map(keyIdA -> keyA)
      )

      val authInfo = (for {
        authToken <- authService.authInfoToToken(validAuthInfo)
        tamperedToken = tamper(authToken)
        result <- authService.validateAuthToken(tamperedToken)
      } yield {
        result
      }).runA(initialState).valueOr(throw _)

      authInfo must contain value AuthService.MissingField("sub")
      authInfo must contain value AuthService.MissingField("aud")
      authInfo must contain value AuthService.MissingField("iss")
      authInfo must contain value AuthService.MissingField("exp")
      authInfo must contain value AuthService.MissingField("nbf")
      authInfo must contain value AuthService.MissingField("iat")
      authInfo must contain value AuthService.MissingField("scp")
    }

    Scenario("validating a token that has fields of the wrong type should fail") {
      def tamper(token: AuthToken): AuthToken = {
        token.copy(body = Base64.getEncoder.encodeToString(
          """{
            |"sub": 1,
            |"exp": "2020-01-01T01:02:02Z",
            |"scp": "write"
            |}""".stripMargin.getBytes()))
      }

      val initialState = InMemoryState(
        keys = Map(keyIdA -> keyA)
      )

      val authInfo = (for {
        authToken <- authService.authInfoToToken(validAuthInfo)
        tamperedToken = tamper(authToken)
        result <- authService.validateAuthToken(tamperedToken)
      } yield {
        result
      }).runA(initialState).valueOr(throw _)

      authInfo must contain value AuthService.InvalidField("sub")
      authInfo must contain value AuthService.InvalidField("exp")
      authInfo must contain value AuthService.InvalidField("scp")
    }

    Scenario("validating a token that has a tampered body should fail") {
      def tamper(token: AuthToken): AuthToken = {
        import ujson._
        val newBody = {
          val json = read(Base64.getDecoder.decode(token.body)).obj
          json.update("scp", List(Str("admin"))) // grant self admin?
          Base64.getEncoder.encodeToString(write(json).getBytes())
        }
        token.copy(body = newBody)
      }

      val initialState = InMemoryState(
        keys = Map(keyIdA -> keyA)
      )

      val authInfo = (for {
        authToken <- authService.authInfoToToken(validAuthInfo)
        tamperedToken = tamper(authToken)
        result <- authService.validateAuthToken(tamperedToken)
      } yield {
        result
      }).runA(initialState).valueOr(throw _)

      authInfo must contain value AuthService.InvalidSignature
    }

    Scenario("validating a token that has a tampered signature should fail") {
      def tamper(token: AuthToken): AuthToken = {
        val newSignature = AuthService.sign(token.header, token.body, keyB)
        token.copy(signature = newSignature)
      }

      val initialState = InMemoryState(
        keys = Map(keyIdA -> keyA)
      )

      val authInfo = (for {
        authToken <- authService.authInfoToToken(validAuthInfo)
        tamperedToken = tamper(authToken)
        result <- authService.validateAuthToken(tamperedToken)
      } yield {
        result
      }).runA(initialState).valueOr(throw _)

      authInfo must contain value AuthService.InvalidSignature
    }

    Scenario("validating a token that is expired should fail") {
      pending
    }

    Scenario("validatinga token that is early should fail") {
      pending
    }
  }
}
