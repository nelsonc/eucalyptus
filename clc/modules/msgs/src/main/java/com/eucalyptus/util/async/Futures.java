/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Copyright (C) 2009 Google Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *   or implied.  See the License for the specific language governing
 *   permissions and limitations under the License.
 ************************************************************************/

package com.eucalyptus.util.async;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import org.apache.log4j.Logger;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.concurrent.GenericCheckedListenableFuture;
import com.eucalyptus.util.concurrent.ListenableFuture;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.not;

public class Futures {
  private static Logger LOG = Logger.getLogger( Futures.class );
  enum WaitForResults implements Predicate<Map.Entry<?, Future<?>>> {
    INSTANCE;
    @Override
    public boolean apply( final Map.Entry<?, Future<?>> input ) {
      try {
        final Object result = input.getValue( ).get( );
        LOG.trace( "Operation succeeded for " + result );
        return true;
      } catch ( final InterruptedException ex ) {
        Thread.currentThread( ).interrupt( );
      } catch ( final Exception ex ) {
        Logs.extreme( ).trace( ex, ex );
      }
      return false;
    }
    
  }
  
  private static <T> Predicate<Map.Entry<T, Future<T>>> waitForResults( ) {
    Predicate func = WaitForResults.INSTANCE;
    return ( Predicate<Map.Entry<T, Future<T>>> ) func;
  }
  
  public static <T> Map<T, Future<T>> waitAll( Map<T, Future<T>> futures ) {
    Predicate<Map.Entry<T, Future<T>>> func = waitForResults( );
    Map<T, Future<T>> res = Maps.filterEntries( futures, func );
    return res;
  }
  
  public static <T> RunnableFuture<T> resultOf( Callable<T> call ) {
    return new FutureTask<T>( call );
  }
  
  public static <T> CheckedListenableFuture<T> newGenericeFuture( ) {
    return new GenericCheckedListenableFuture<T>( );
  }
  
  public static <T> CheckedListenableFuture<T> predestinedFuture( final T initValue ) {
    return new GenericCheckedListenableFuture<T>( ) {
      {
        this.set( initValue );
      }
    };
  }
  
  public static <T> CheckedListenableFuture<T> predestinedFailedFuture( final Throwable exValue ) {
    return new GenericCheckedListenableFuture<T>( ) {
      {
        this.setException( exValue );
      }
    };
  }
  
  /**
   * Returns a new {@code Callable} which will execute {@code firstCall} and, if it succeeds,
   * {@code secondCall} in sequence. The resulting {@code resultFuture} will return one of:
   * <ol>
   * <li>{@link Future#get()} returns the result of {@code secondCall}'s future result.</li>
   * <li>{@link Future#get()} throws the exception which caused {@code firstCall} to fail -- in this
   * case {@code secondCall} is not executed.</li>
   * <li>{@link Future#get()} throws the exception which caused {@code secondCall} to fail.</li>
   * </ol>
   * 
   * @param <P>
   * @param firstCall
   * @param secondCall
   * @return resultFuture
   */
  public static <T extends ListenableFuture<V>, V> Callable<T> combine( final Callable<T> firstCall, final Callable<T> secondCall ) {
    final CheckedListenableFuture<V> resultFuture = Futures.newGenericeFuture( );
    final CheckedListenableFuture<T> intermediateFuture = Futures.newGenericeFuture( );
    
    final Callable<T> chainingCallable = new Callable<T>( ) {
      @Override
      public String toString( ) {
        return Callable.class.getSimpleName( ) + ":["
               + firstCall.toString( )
               + "] ==> "
               + secondCall.toString( );
      }
      
      @Override
      public T call( ) {
        try {
          try {
            final T res = firstCall.call( );
            intermediateFuture.set( res );
          } catch ( Exception ex ) {
            resultFuture.setException( ex );
            intermediateFuture.setException( ex );
          }
          Threads.lookup( Empyrean.class, Futures.class ).submit( new Runnable( ) {
            @Override
            public String toString( ) {
              return Runnable.class.getSimpleName( ) + ":"
                     + firstCall.toString( )
                     + " ==> ["
                     + secondCall.toString( )
                     + "]";
            }
            
            @Override
            public void run( ) {
              try {
                intermediateFuture.get( ).get( );
                try {
                  T res2 = secondCall.call( );
                  resultFuture.set( res2.get( ) );
                } catch ( Exception ex ) {
                  resultFuture.setException( ex );
                }
              } catch ( InterruptedException ex ) {
                LOG.error( ex );
                Thread.currentThread( ).interrupt( );
                resultFuture.setException( ex );
              } catch ( ExecutionException ex ) {
                resultFuture.setException( ex.getCause( ) );
              } catch ( Exception ex ) {
                resultFuture.setException( ex );
              }
            }
          } ).get( );
        } catch ( InterruptedException ex1 ) {
          Thread.currentThread( ).interrupt( );
          resultFuture.setException( ex1 );
        } catch ( RejectedExecutionException ex1 ) {
          resultFuture.setException( ex1 );
        } catch ( Exception ex1 ) {
          resultFuture.setException( ex1 );
        }
        return ( T ) resultFuture;
      }
    };
    return chainingCallable;
  }
  
  public static <P> Callable<CheckedListenableFuture<P>> sequence( final Callable<CheckedListenableFuture<P>>... callables ) {
    assertThat( callables, not( emptyArray( ) ) );
    if ( callables.length == 1 ) {
      return callables[0];
    } else if ( callables.length == 2 ) {
      return Futures.combine( callables[0], callables[1] );
    } else {
      final Callable<CheckedListenableFuture<P>>[] nextCallables = Arrays.copyOfRange( callables, 1, callables.length );
      nextCallables[0] = Futures.combine( callables[0], callables[1] );
      return sequence( nextCallables );
    }
  }
  
}
