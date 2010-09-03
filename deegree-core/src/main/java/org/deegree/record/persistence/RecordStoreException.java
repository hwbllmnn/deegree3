//$Header: /deegreerepository/deegree/src/org/deegree/io/datastore/Datastore.java,v 1.28 2007/01/16 13:58:34 mschneider Exp $
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2009 by:
 Department of Geography, University of Bonn
 and
 lat/lon GmbH

 This library is free software; you can redistribute it and/or modify it under
 the terms of the GNU Lesser General Public License as published by the Free
 Software Foundation; either version 2.1 of the License, or (at your option)
 any later version.
 This library is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 details.
 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation, Inc.,
 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Contact information:

 lat/lon GmbH
 Aennchenstr. 19, 53177 Bonn
 Germany
 http://lat-lon.de/

 Department of Geography, University of Bonn
 Prof. Dr. Klaus Greve
 Postfach 1147, 53001 Bonn
 Germany
 http://www.geographie.uni-bonn.de/deegree/

 e-mail: info@deegree.org
 ----------------------------------------------------------------------------*/

package org.deegree.record.persistence;

/**
 * Indicates an exception that occured in the record persistence layer.
 * 
 * @author <a href="mailto:thomas@lat-lon.de">Steffen Thomas</a>
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider</a>
 * @author last edited by: $Author: mschneider $
 * 
 * @version $Revision: 18942 $, $Date: 2009-08-05 19:16:43 +0200 (Mi, 05 Aug 2009) $
 */
public class RecordStoreException extends Exception {

    private static final long serialVersionUID = -8171919093492328054L;

    /**
     * Creates a new {@link RecordStoreException} without detail message.
     */
    public RecordStoreException() {
        super();
    }

    /**
     * Creates a new {@link RecordStoreException} with detail message.
     * 
     * @param message
     *            detail message
     */
    public RecordStoreException( String message ) {
        super( message );
    }

    /**
     * Creates a new {@link RecordStoreException} which wraps the causing exception.
     * 
     * @param cause
     */
    public RecordStoreException( Throwable cause ) {
        super( cause );
    }

    /**
     * Creates a new {@link RecordStoreException} which wraps the causing exception and provides a detail message.
     * 
     * @param message
     * @param cause
     */
    public RecordStoreException( String message, Throwable cause ) {
        super( message, cause );
    }
}
