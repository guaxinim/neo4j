/*
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.compiler.v2_3.ast.conditions

import org.neo4j.cypher.internal.compiler.v2_3.Ref
import org.neo4j.cypher.internal.compiler.v2_3.ast.Identifier
import org.neo4j.cypher.internal.compiler.v2_3.tracing.rewriters.Condition

case object noReferenceEqualityAmongIdentifiers extends Condition {
  def apply(that: Any): Seq[String] = {
    val ids = collectNodesOfType[Identifier].apply(that).map(Ref[Identifier])
    ids.groupBy(x => x).collect {
      case (id, others) if others.size > 1 => s"The instance ${id.value} is used ${others.size} times"
    }.toSeq
  }

  override def name: String = productPrefix
}

