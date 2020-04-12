package me.enkode.test_utils

import cats.data.ValidatedNel
import org.scalatest.enablers.ValueMapping

package object implicits {

  implicit def validedNelValueMapping[E, A]: ValueMapping[ValidatedNel[E, A]] = {
    (validated: ValidatedNel[E, A], value: Any) => {
      validated
        .leftMap(_.exists(_ == value))
        .map(_ == value)
        .fold(identity, identity)
    }
  }
}
