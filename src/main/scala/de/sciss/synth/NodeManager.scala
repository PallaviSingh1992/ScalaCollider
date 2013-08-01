/*
 *  NodeManager.scala
 *  (ScalaCollider)
 *
 *  Copyright (c) 2008-2013 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either
 *  version 2, june 1991 of the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License (gpl.txt) along with this software; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.synth

import de.sciss.model.impl.ModelImpl
import de.sciss.model.Model

object NodeManager {
  type Listener = Model.Listener[NodeManager.Update]

  sealed trait Update

  abstract sealed class NodeChange extends Update {
    def node: Node
    def info: message.NodeInfo.Data
  }

  final case class NodeGo   (node: Node, info: message.NodeInfo.Data) extends NodeChange
  final case class NodeEnd  (node: Node, info: message.NodeInfo.Data) extends NodeChange
  final case class NodeOn   (node: Node, info: message.NodeInfo.Data) extends NodeChange
  final case class NodeOff  (node: Node, info: message.NodeInfo.Data) extends NodeChange
  final case class NodeMove (node: Node, info: message.NodeInfo.Data) extends NodeChange

  case object Cleared extends Update
}

final class NodeManager(val server: Server) extends ModelImpl[NodeManager.Update] {
  import NodeManager._

  private var nodes: Map[Int, Node] = _
  private val sync     = new AnyRef

  // ---- constructor ----
  clear()

  //      if( server.isRunning ) {
  //         val defaultGroup = server.defaultGroup
  //         nodes += defaultGroup.id -> defaultGroup
  //      }

  def nodeChange(e: message.NodeChange): Unit =
    e match {
      case message.NodeGo(nodeID, info) =>
        val node = nodes.get(nodeID) getOrElse {
          if ( /* autoAdd && */ nodes.contains(info.parentID)) {
            val created = info match {
              case ee: message.NodeInfo.SynthData => Synth(server, nodeID)
              case ee: message.NodeInfo.GroupData => Group(server, nodeID)
            }
            register(created)
            created
          } else return
        }
        dispatchBoth(NodeGo(node, info))

      case message.NodeEnd(nodeID, info) =>
        // println(s"---- NodeEnd: ${nodes.get(nodeID)}")
        nodes.get(nodeID).foreach { node =>
          unregister(node)
          dispatchBoth(NodeEnd(node, info))
        }

      case message.NodeOff(nodeID, info) =>
        nodes.get(e.nodeID).foreach { node =>
          dispatchBoth(NodeOff(node, info))
        }

      case message.NodeOn(nodeID, info) =>
        nodes.get(e.nodeID).foreach { node =>
          dispatchBoth(NodeOn(node, info))
        }

      case message.NodeMove(nodeID, info) =>
        nodes.get(e.nodeID).foreach { node =>
          dispatchBoth(NodeMove(node, info))
        }

      case _ =>
	}

  private def dispatchBoth(change: NodeChange): Unit = {
    dispatch(change)
    change.node.updated(change)
  }

  // eventually this should be done automatically
  // by the message dispatch management
  def register(node: Node): Unit =
    sync.synchronized {
      // println(s"---- register node: $node")
      nodes += node.id -> node
    }

  def unregister(node: Node): Unit =
    sync.synchronized {
      // println(s"---- unregister node: $node")
      nodes -= node.id
    }

  def getNode(id: Int): Option[Node] = nodes.get(id)  // sync.synchronized { }

  def clear(): Unit = {
    val rootNode = server.rootNode // new Group( server, 0 )
    sync.synchronized {
      nodes = Map(rootNode.id -> rootNode)
    }
    dispatch(Cleared)
  }
}