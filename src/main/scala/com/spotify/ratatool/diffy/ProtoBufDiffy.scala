/*
 * Copyright 2016 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.ratatool.diffy

import com.google.protobuf.Descriptors.FieldDescriptor.JavaType
import com.google.protobuf.Descriptors.{Descriptor, FieldDescriptor}
import com.google.protobuf.GeneratedMessage

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

object ProtoBufDiffy {

  def apply[T <: GeneratedMessage : ClassTag](x: T, y: T, descriptor: Descriptor): Seq[Delta] =
    diff(x, y, descriptor.getFields.asScala, "")

  def apply[T <: GeneratedMessage : ClassTag](x: T, y: T): Seq[Delta] = {
    val cls = implicitly[ClassTag[T]].runtimeClass
    val descriptor = cls.getMethod("getDescriptor").invoke(null).asInstanceOf[Descriptor]
    apply(x, y, descriptor)
  }

  private def diff(x: GeneratedMessage, y: GeneratedMessage,
                   fields: Seq[FieldDescriptor], root: String): Seq[Delta] = {
    def getField(m: GeneratedMessage, f: FieldDescriptor): Any =
      if (m.hasField(f)) m.getField(f) else null

    fields.flatMap { f=>
      val name = f.getName
      val fullName = if (root.isEmpty) name else root + "." + name
      f.getJavaType match {
        case JavaType.MESSAGE =>
          val a = getField(x, f).asInstanceOf[GeneratedMessage]
          val b = getField(y, f).asInstanceOf[GeneratedMessage]
          if (a == null && b == null) {
            Nil
          } else if (a == null || b == null) {
            Seq(Delta(fullName, a, b))
          } else {
            diff(a, b, f.getMessageType.getFields.asScala, fullName)
          }
        case _ =>
          val a = x.getField(f)
          val b = y.getField(f)
          if (a == b) Nil else Seq(Delta(fullName, a, b))
      }
    }
  }

}
