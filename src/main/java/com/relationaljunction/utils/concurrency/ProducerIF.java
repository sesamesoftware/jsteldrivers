package com.relationaljunction.utils.concurrency;

/**
 * <p>Title: StelsMDB JDBC driver</p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2008</p>
 *
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 1.0
 */
public interface ProducerIF<E> {

  E produce() throws Exception;

}
