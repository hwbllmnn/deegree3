//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2010 by:
 - Department of Geography, University of Bonn -
 and
 - lat/lon GmbH -

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
package org.deegree.services.wms.model.layers;

import static org.deegree.commons.tom.primitive.PrimitiveType.DATE;
import static org.deegree.commons.tom.primitive.PrimitiveType.STRING;
import static org.deegree.commons.utils.ArrayUtils.splitAsDoubles;
import static org.deegree.commons.utils.time.DateUtils.formatISO8601Date;
import static org.deegree.feature.types.property.GeometryPropertyType.CoordinateDimension.DIM_2;
import static org.deegree.feature.types.property.GeometryPropertyType.GeometryType.GEOMETRY;
import static org.deegree.feature.types.property.ValueRepresentation.BOTH;
import static org.slf4j.LoggerFactory.getLogger;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.deegree.commons.utils.ComparablePair;
import org.deegree.commons.utils.Pair;
import org.deegree.cs.CRS;
import org.deegree.feature.FeatureCollection;
import org.deegree.feature.GenericFeature;
import org.deegree.feature.GenericFeatureCollection;
import org.deegree.feature.property.GenericProperty;
import org.deegree.feature.property.Property;
import org.deegree.feature.property.SimpleProperty;
import org.deegree.feature.types.FeatureType;
import org.deegree.feature.types.GenericFeatureType;
import org.deegree.feature.types.property.GeometryPropertyType;
import org.deegree.feature.types.property.PropertyType;
import org.deegree.feature.types.property.SimplePropertyType;
import org.deegree.geometry.Envelope;
import org.deegree.geometry.GeometryFactory;
import org.deegree.rendering.r2d.Java2DRenderer;
import org.deegree.rendering.r2d.se.unevaluated.Style;
import org.deegree.rendering.r2d.se.unevaluated.Symbolizer;
import org.deegree.rendering.r2d.styling.PolygonStyling;
import org.deegree.rendering.r2d.styling.components.Fill;
import org.deegree.rendering.r2d.styling.components.Stroke;
import org.deegree.services.controller.FrontControllerStats;
import org.deegree.services.controller.OGCFrontController;
import org.deegree.services.controller.wms.ops.GetFeatureInfo;
import org.deegree.services.controller.wms.ops.GetMap;
import org.deegree.services.wms.WMSException.InvalidDimensionValue;
import org.deegree.services.wms.WMSException.MissingDimensionValue;
import org.slf4j.Logger;

/**
 * <code>StatisticsLayer</code>
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class StatisticsLayer extends Layer {

    private static final Logger LOG = getLogger( StatisticsLayer.class );

    private static final GeometryFactory fac = new GeometryFactory();

    private static final String ns = "http://www.deegree.org/wms-statistics";

    private static final GenericFeatureType featureType;

    private static final SimplePropertyType queryProp = new SimplePropertyType( new QName( ns, "query_string" ), 1, 1,
                                                                                STRING, false, null );

    private static final SimplePropertyType timeProp = new SimplePropertyType( new QName( ns, "time" ), 1, 1, DATE,
                                                                               false, null );

    private static final GeometryPropertyType boxProp = new GeometryPropertyType( new QName( ns, "boundingbox" ), 1, 1,
                                                                                  GEOMETRY, DIM_2, false, null, BOTH );
    static {
        List<PropertyType> props = new ArrayList<PropertyType>();
        props.add( queryProp );
        props.add( timeProp );
        props.add( boxProp );
        featureType = new GenericFeatureType( new QName( ns, "request" ), props, false );
    }

    /**
     * @param parent
     */
    public StatisticsLayer( Layer parent ) {
        super( "statistics", "WMS Request Statistics", parent );
    }

    @Override
    public FeatureType getFeatureType() {
        return featureType;
    }

    @Override
    public Pair<FeatureCollection, LinkedList<String>> getFeatures( GetFeatureInfo fi, Style style )
                            throws MissingDimensionValue, InvalidDimensionValue {
        GenericFeatureCollection col = new GenericFeatureCollection();
        for ( ComparablePair<Long, String> req : FrontControllerStats.getKVPRequests() ) {
            if ( req.second.toUpperCase().indexOf( "REQUEST=GETMAP" ) != -1 ) {
                try {
                    Map<String, String> map = OGCFrontController.getNormalizedKVPMap( req.second, "UTF-8" );
                    if ( map.get( "LAYERS" ).equals( "statistics" ) ) {
                        continue;
                    }
                    double[] vals = splitAsDoubles( map.get( "BBOX" ), "," );
                    Envelope box = fac.createEnvelope( vals[0], vals[1], vals[2], vals[3], new CRS( map.get( "SRS" ) ) );
                    if ( !box.intersects( fi.getClickBox() ) ) {
                        continue;
                    }
                    List<Property> props = new ArrayList<Property>();
                    props.add( new SimpleProperty( queryProp, req.second, STRING ) );
                    props.add( new SimpleProperty( timeProp, formatISO8601Date( new Date( req.first ) ), DATE ) );
                    props.add( new GenericProperty( boxProp, box ) );

                    col.add( new GenericFeature( featureType, null, props, null ) );

                } catch ( UnsupportedEncodingException e ) {
                    LOG.trace( "Stack trace:", e );
                }
            }
        }
        return new Pair<FeatureCollection, LinkedList<String>>( col, new LinkedList<String>() );
    }

    @Override
    public LinkedList<String> paintMap( Graphics2D g, GetMap gm, Style style )
                            throws MissingDimensionValue, InvalidDimensionValue {
        PolygonStyling ps = new PolygonStyling();
        ps.stroke = new Stroke();
        ps.stroke.color = Color.black;
        ps.fill = new Fill();
        ps.fill.color = new Color( 0x800000ff, true );
        if ( style == null ) {
            style = new Style( new Symbolizer<PolygonStyling>( ps, null, null, null, -1, -1 ), null, null, null );
        }

        Java2DRenderer renderer = new Java2DRenderer( g, gm.getWidth(), gm.getHeight(), gm.getBoundingBox(),
                                                      gm.getPixelSize() );
        for ( ComparablePair<Long, String> req : FrontControllerStats.getKVPRequests() ) {
            if ( req.second.toUpperCase().indexOf( "REQUEST=GETMAP" ) != -1 ) {
                try {
                    Map<String, String> map = OGCFrontController.getNormalizedKVPMap( req.second, "UTF-8" );
                    if ( map.get( "LAYERS" ).equals( "statistics" ) ) {
                        continue;
                    }
                    double[] vals = splitAsDoubles( map.get( "BBOX" ), "," );
                    Envelope box = fac.createEnvelope( vals[0], vals[1], vals[2], vals[3], new CRS( map.get( "SRS" ) ) );
                    if ( !box.intersects( gm.getBoundingBox() ) ) {
                        continue;
                    }
                    List<Property> props = new ArrayList<Property>();
                    props.add( new SimpleProperty( queryProp, req.second, STRING ) );
                    props.add( new SimpleProperty( timeProp, formatISO8601Date( new Date( req.first ) ), DATE ) );
                    props.add( new GenericProperty( boxProp, box ) );

                    render( new GenericFeature( featureType, null, props, null ), style, renderer, null, gm.getScale() );

                } catch ( UnsupportedEncodingException e ) {
                    LOG.trace( "Stack trace:", e );
                }
            }
        }
        return new LinkedList<String>();
    }

    @Override
    public String getName() {
        return "statistics";
    }

}
