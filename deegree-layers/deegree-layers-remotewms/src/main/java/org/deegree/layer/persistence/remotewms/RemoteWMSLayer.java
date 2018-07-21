package org.deegree.layer.persistence.remotewms;

import org.apache.logging.log4j.Logger;
import org.deegree.cs.coordinatesystems.ICRS;
import org.deegree.cs.persistence.CRSManager;
import org.deegree.layer.AbstractLayer;
import org.deegree.layer.LayerQuery;
import org.deegree.layer.metadata.LayerMetadata;
import org.deegree.layer.persistence.remotewms.jaxb.ParameterScopeType;
import org.deegree.layer.persistence.remotewms.jaxb.ParameterUseType;
import org.deegree.layer.persistence.remotewms.jaxb.RequestOptionsType;
import org.deegree.layer.persistence.remotewms.jaxb.RequestOptionsType.DefaultCRS;
import org.deegree.layer.persistence.remotewms.jaxb.RequestOptionsType.Parameter;
import org.deegree.protocol.wms.client.WMSClient;
import org.deegree.protocol.wms.ops.GetFeatureInfo;
import org.deegree.protocol.wms.ops.GetMap;
import org.deegree.style.StyleRef;

import java.util.*;

import static java.util.Collections.singletonList;
import static org.apache.logging.log4j.LogManager.getLogger;
import static org.deegree.commons.utils.RequestUtils.replaceParameters;
import static org.deegree.protocol.wms.WMSConstants.WMSRequestType.GetMap;

/**
 * TODO add class documentation here
 * 
 * @author <a href="mailto:schmitz@occamlabs.de">Andreas Schmitz</a>
 * @author last edited by: $Author: stranger $
 * 
 * @version $Revision: $, $Date: $
 */
class RemoteWMSLayer extends AbstractLayer {

    private static final Logger LOG = getLogger( RemoteWMSLayer.class );

    private final WMSClient client;

    private ICRS crs;

    private boolean alwaysUseDefaultCrs;

    private String format;

    private boolean transparent = true;

    private HashMap<String, String> defaultParametersGetMap = new HashMap<String, String>();

    private HashMap<String, String> defaultParametersGetFeatureInfo = new HashMap<String, String>();

    private HashMap<String, String> hardParametersGetMap = new HashMap<String, String>();

    private HashMap<String, String> hardParametersGetFeatureInfo = new HashMap<String, String>();

    private final String originalName;

    RemoteWMSLayer( String originalName, LayerMetadata md, WMSClient client, RequestOptionsType opts ) {
        super( md );
        this.originalName = originalName;
        md.setCascaded( md.getCascaded() + 1 );
        this.client = client;
        if ( opts != null ) {
            if ( opts.getDefaultCRS() != null ) {
                DefaultCRS crs = opts.getDefaultCRS();
                this.crs = CRSManager.getCRSRef( crs.getValue(), true );
                alwaysUseDefaultCrs = crs.isUseAlways();
            }
            if ( opts.getImageFormat() != null ) {
                this.format = opts.getImageFormat().getValue();
                this.transparent = opts.getImageFormat().isTransparent();
            }
            extractParameters( opts.getParameter() );
        }
        // set default values if not configured
        if ( this.crs == null ) {
            this.crs = CRSManager.getCRSRef( client.getCoordinateSystems( originalName ).getFirst() );
        }
        if ( this.format == null ) {
            LinkedList<String> fs = client.getFormats( GetMap );
            if ( fs.contains( "image/png" ) ) {
                format = "image/png";
            } else {
                format = fs.getFirst();
            }
        }
    }

    private void extractParameters( List<Parameter> params ) {
        if ( params != null && !params.isEmpty() ) {
            for ( Parameter p : params ) {
                String name = p.getName();
                String value = p.getValue();
                ParameterUseType use = p.getUse();
                ParameterScopeType scope = p.getScope();
                switch ( use ) {
                case ALLOW_OVERRIDE:
                    switch ( scope ) {
                    case GET_MAP:
                        defaultParametersGetMap.put( name, value );
                        break;
                    case GET_FEATURE_INFO:
                        defaultParametersGetFeatureInfo.put( name, value );
                        break;
                    default:
                        defaultParametersGetMap.put( name, value );
                        defaultParametersGetFeatureInfo.put( name, value );
                        break;
                    }
                    break;
                case FIXED:
                    switch ( scope ) {
                    case GET_MAP:
                        hardParametersGetMap.put( name, value );
                        break;
                    case GET_FEATURE_INFO:
                        hardParametersGetFeatureInfo.put( name, value );
                        break;
                    default:
                        hardParametersGetMap.put( name, value );
                        hardParametersGetFeatureInfo.put( name, value );
                        break;
                    }
                    break;
                }
            }
        }
    }

    @Override
    public RemoteWMSLayerData mapQuery( LayerQuery query, List<String> headers ) {        
        Map<String, String> extraParams = new HashMap<String, String>();
        replaceParameters( extraParams, query.getParameters(), defaultParametersGetMap, hardParametersGetMap );
        ICRS crs = this.crs;
        if ( !alwaysUseDefaultCrs ) {
            ICRS envCrs = query.getEnvelope().getCoordinateSystem();
            if ( client.getCoordinateSystems( originalName ).contains( envCrs.getAlias() ) ) {
                crs = envCrs;
            }
        }

        GetMap gm = new GetMap( singletonList( originalName ), query.getWidth(), query.getHeight(),
                                query.getEnvelope(), crs, format, transparent );
        return new RemoteWMSLayerData( client, gm, extraParams );
    }

    @Override
    public RemoteWMSLayerData infoQuery( LayerQuery query, List<String> headers ) {
        Map<String, String> extraParams = new HashMap<String, String>();
        replaceParameters( extraParams, query.getParameters(), defaultParametersGetFeatureInfo,
                           hardParametersGetFeatureInfo );

        ICRS crs = this.crs;
        if ( !alwaysUseDefaultCrs ) {
            ICRS envCrs = query.getEnvelope().getCoordinateSystem();
            if ( client.getCoordinateSystems( originalName ).contains( envCrs.getAlias() ) ) {
                crs = envCrs;
            }
        }

        GetFeatureInfo gfi = new GetFeatureInfo( Collections.singletonList( originalName ), query.getWidth(),
                                                 query.getHeight(), query.getX(), query.getY(), query.getEnvelope(),
                                                 crs, query.getFeatureCount() );
        return new RemoteWMSLayerData( client, gfi, extraParams );
    }

    @Override
    public boolean isStyleApplicable( StyleRef style ) {
        if ( "default".equals( style.getName() ) ) {
            return true;
        }
        return resolveStyleRef( style ) != null;
    }

}
