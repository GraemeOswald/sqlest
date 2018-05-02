/*
 * Copyright 2014 JHC Systems Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sqlest.ast

sealed trait Insert extends Command

case class InsertValues(into: Table, setterLists: Seq[Seq[Setter[_, _]]]) extends Insert {
  def newRecord = new InsertNewRecordBuilder

  def columns = setterLists.head.map(_.column)

  def bulkInsert[A](items: Seq[A])(setters: A => Seq[Setter[_, _]]) = {
    items.foldLeft(this) {
      case (insertValues, item) =>
        insertValues.newRecord.set(setters(item))
    }
  }

  class InsertNewRecordBuilder {
    def set(newRecordSetters: Setter[_, _]*) = {
      if (columns != newRecordSetters.map(_.column)) throw new AssertionError(s"Must set the same columns as previous insert rows")
      InsertValues(into, setterLists :+ newRecordSetters)
    }

    def set(newRecordSetters: Seq[Setter[_, _]])(implicit d: DummyImplicit) = {
      if (columns != newRecordSetters.map(_.column)) throw new AssertionError(s"Must set the same columns as previous insert rows")
      InsertValues(into, setterLists :+ newRecordSetters)
    }
  }

  def values(setters: Setter[_, _]*) = {
    if (columns != setters.map(_.column)) throw new AssertionError(s"Cannot insert value to the columns declared")
    InsertValues(into, setterLists :+ setters)
  }

  def values(setters: Seq[Setter[_, _]])(implicit d: DummyImplicit) = {
    if (columns != setters.map(_.column)) throw new AssertionError(s"Cannot insert value to the columns declared")
    InsertValues(into, setterLists :+ setters)
  }

  def values(setterLists: Seq[Seq[Setter[_, _]]])(implicit d1: DummyImplicit, d2: DummyImplicit) = {
    if (!setterLists.forall(columns == _.map(_.column))) throw new AssertionError(s"Cannot insert value to the columns declared")
    InsertValues(into, setterLists)
  }
}

// TODO - Constrain columns to be related type to select
case class InsertFromSelect[A: AliasedColumns](into: Table, columns: Seq[TableColumn[_]], select: Select[A, _ <: Relation]) extends Insert
