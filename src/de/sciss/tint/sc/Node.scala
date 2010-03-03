/*
 *  Node.scala
 *  Tintantmare
 *
 *  Copyright (c) 2008-2009 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 */

package de.sciss.tint.sc

import scala.collection.immutable.HashMap
import collection.mutable.ListBuffer

import de.sciss.scalaosc.OSCMessage

/**
 * 	@author		Hanns Holger Rutz
 *	@version	0.13, 13-Jan-10
 */

sealed abstract class AddAction( val id: Int )

case object addToHead   extends AddAction( 0 )
case object addToTail   extends AddAction( 1 )
case object addBefore   extends AddAction( 2 )
case object addAfter    extends AddAction( 3 )
case object addReplace  extends AddAction( 4 )

/**
 * 	@author		Hanns Holger Rutz
 *    @version    0.13, 03-Mar-10
 */
class Node( val server: Server, val id: Int )
extends Model
{
	var group : Group = null
	var isPlaying	= false
	var isRunning = false

	def register: Unit = register( false )
	def register( assumePlaying: Boolean ) {
//  	NodeWatcher.register( this, assumePlaying )
       server.nodeMgr.register( this )
  	}

   def onGo( thunk: => Unit ) {
      register
      lazy val l: (AnyRef) => Unit = _ match {
         case NodeManager.NodeGo( _, _ ) => {
            removeListener( l )
            thunk
         }
      }
      addListener( l )
   }

   def onEnd( thunk: => Unit ) {
      register
      lazy val l: (AnyRef) => Unit = _ match {
         case NodeManager.NodeEnd( _, _ ) => {
            removeListener( l )
            thunk
         }
      }
      addListener( l )
   }

   protected[sc] def updated( change: NodeManager.NodeChange ) {
      // XXX need to update isPlaying, isRunning etc.
      dispatch( change )
   }

	def free : Node = free( true )

	def free( sendFlag: Boolean ) : Node = {
  		if( sendFlag ) server.sendMsg( freeMsg )
  		group = null
  		isPlaying = false
  		isRunning = false
  		this
  	}
  
//  private def asArray( o: Object ) : Array[Object] = Seq( o ).toArray
//  private def asArray( o: Int ) : Array[Object] = Seq( o.asInstanceOf[AnyRef] ).toArray
  
  	def freeMsg = OSCMessage( "/n_free", id )

  	def run : Node = run( true )
  
  	def run( flag: Boolean ) : Node = {
  		server.sendMsg( runMsg( flag ))
  		this
  	}
	
  	def runMsg : OSCMessage = runMsg( true )

	// XXX should add automatic boolean conversion
  	def runMsg( flag: Boolean ) = OSCMessage( "/n_run", id, if( flag ) 1 else 0 )
  
  	def set( pairs: Tuple2[ Any, Float ]*) : Node = {
  		server.sendMsg( setMsg( pairs: _* ))
  		this
  	}
	
  	def setMsg( pairs: Tuple2[ Any, Float ]*) : OSCMessage = {
  		val args = new Array[ Any ]( (pairs.size << 1) + 1 )
  		args( 0 ) = id
  		var i = 1
  		pairs.foreach { tuple => {
  			args( i ) = tuple._1
  		    i = i + 1
  			args( i ) = tuple._2
  		    i = i + 1
  		}}
  		OSCMessage( "/n_set", args:_* )
  	}

  	def setn( pairs: Tuple2[ Any, Seq[ Float ]]*) : Node = {
  		server.sendMsg( setnMsg( pairs: _* ))
  		this
  	}
	
  	def setnMsg( pairs: Tuple2[ Any, Seq[ Float ]]*) : OSCMessage = {
  		val args = new ListBuffer[ Any ]()
  		args += id
  		pairs.foreach (pair => {
  			args += pair._1
            args += pair._2.size
            pair._2.foreach { value => args += value }
  		})
  		OSCMessage( "/n_setn", args: _* )
  	}

  	def trace : Node = {
  		server.sendMsg( "/n_trace", id )
  		this
  	}

  	def release : Node = release( None )
  
  	def release( releaseTime: Option[ Float ]) : Node = {
  		server.sendMsg( releaseMsg( releaseTime ))
  		this
  	}

  	def releaseMsg : OSCMessage = releaseMsg( None )
  
  	// assumes a control called 'gate' in the synth
  	def releaseMsg( releaseTime: Option[ Float ]) : OSCMessage = {
  		val value = releaseTime.map( -1.0f - _ ).getOrElse( 0.0f )
  		setMsg( "gate" -> value )
	}

  	def map( pairs: Tuple2[ Any, Int ]*) : Node = {
  		server.sendMsg( mapMsg( pairs: _* ))
  		this
  	}
  
  	def mapMsg( pairs: Tuple2[ Any, Int ]*) : OSCMessage = {
  		val args = new Array[ Any ]( (pairs.size << 1) + 1 )
  		args( 0 ) = id
	    var i = 1
	    pairs.foreach { tuple => {
	    	args( i ) = tuple._1
	    	i = i + 1
	    	args( i ) = tuple._2
	    	i = i + 1
	    }}
  		OSCMessage( "/n_map", args:_* )
  	}

  	def mapn( triplets: Tuple3[ Any, Int, Int ]*) : Unit = {
  		server.sendMsg( mapnMsg( triplets: _* ))
  		this
  	}
  	
  	def mapnMsg( triplets: Tuple3[ Any, Int, Int ]*) : OSCMessage = {
  		val args = new Array[ Any ]( triplets.size * 3 + 1 )
  		args( 0 ) = id
        var i = 1
        triplets.foreach { trip => {
        	args( i )     = trip._1
            args( i + 1 ) = trip._2
            args( i + 2 ) = trip._3
            i = i + 3
        }}
  		OSCMessage( "/n_mapn", args:_* )
  	}

  	def fill( triplets: Tuple3[ Any, Int, Float ]*) : Node = {
  		server.sendMsg( fillMsg( triplets: _* ))
  		this
  	}
	
  	def fillMsg( triplets: Tuple3[ Any, Int, Float ]*) : OSCMessage = {
  		val args = new Array[ Any ]( triplets.size * 3 + 1 )
  		args( 0 ) = id
        var i = 1
        triplets.foreach { trip => {
        	args( i )     = trip._1
	        args( i + 1 ) = trip._2
	        args( i + 2 ) = trip._3
	        i = i + 3
        }}
    	OSCMessage( "/n_fill", args:_* )
  	}

    def moveAfterMsg( node: Node ) : OSCMessage = {
		group = node.group
		OSCMessage( "/n_after", id, node.id )
	}
 
  	def moveToHeadMsg( group: Group ) : OSCMessage = group.moveNodeToHeadMsg( this )	
  	def moveToTailMsg( group: Group ) : OSCMessage = group.moveNodeToTailMsg( this )
}