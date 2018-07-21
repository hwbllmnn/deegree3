/*----------------------------------------------------------------------------
 This file is part of deegree
 Copyright (C) 2001-2013 by:
 - Department of Geography, University of Bonn -
 and
 - lat/lon GmbH -
 and
 - Occam Labs UG (haftungsbeschränkt) -
 and others

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

 e-mail: info@deegree.org
 website: http://www.deegree.org/
----------------------------------------------------------------------------*/
package org.deegree.layer.persistence.coverage;

import org.apache.logging.log4j.Logger;
import org.deegree.layer.persistence.LayerStore;
import org.deegree.layer.persistence.coverage.jaxb.CoverageLayers;
import org.deegree.workspace.ResourceBuilder;
import org.deegree.workspace.ResourceMetadata;
import org.deegree.workspace.Workspace;

import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * This class is responsible for building coverage layer stores.
 * 
 * @author <a href="mailto:schmitz@occamlabs.de">Andreas Schmitz</a>
 * 
 * @since 3.4
 */
public class CoverageLayerStoreBuilder implements ResourceBuilder<LayerStore> {

    private static final Logger LOG = getLogger( CoverageLayerStoreBuilder.class );

    private CoverageLayers cfg;

    private Workspace workspace;

    private ResourceMetadata<LayerStore> metadata;

    public CoverageLayerStoreBuilder( CoverageLayers cfg, Workspace workspace, ResourceMetadata<LayerStore> metadata ) {
        this.cfg = cfg;
        this.workspace = workspace;
        this.metadata = metadata;
    }

    @Override
    public LayerStore build() {
        if ( cfg.getAutoLayers() != null ) {
            LOG.debug( "Using auto configuration for coverage layers." );
            AutoCoverageLayerBuilder builder = new AutoCoverageLayerBuilder( workspace, metadata );
            return builder.createFromAutoLayers( cfg.getAutoLayers() );
        }

        LOG.debug( "Using manual configuration for coverage layers." );

        ManualCoverageLayerBuilder builder = new ManualCoverageLayerBuilder( workspace, metadata );
        return builder.buildManual( cfg );
    }

}
