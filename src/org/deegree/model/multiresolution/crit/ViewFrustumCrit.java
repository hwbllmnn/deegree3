//$HeadURL: svn+ssh://mschneider@svn.wald.intevation.org/deegree/base/trunk/resources/eclipse/files_template.xml $
/*----------------    FILE HEADER  ------------------------------------------
 This file is part of deegree.
 Copyright (C) 2001-2009 by:
 Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 Lesser General Public License for more details.
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 Contact:

 Andreas Poth
 lat/lon GmbH
 Aennchenstr. 19
 53177 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Prof. Dr. Klaus Greve
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: greve@giub.uni-bonn.de
 ---------------------------------------------------------------------------*/
package org.deegree.model.multiresolution.crit;

import org.deegree.model.multiresolution.Arc;
import org.deegree.rendering.r3d.Frustum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link LODCriterion} for specifying LODs that are optimized for perspective visualization.
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$
 */
public class ViewFrustumCrit implements LODCriterion {

    private static final Logger LOG = LoggerFactory.getLogger( ViewFrustumCrit.class );

    private float pixelError;

    private int screenX;

    private int screenY;

    private Frustum viewRegion;

    private BoxPointDistance distance = new BoxPointDistance();

    /**
     * Creates a new {@link ViewFrustumCrit} instance.
     * 
     * @param viewVolume
     *            specifies the visible space volume (viewer position, view direction, etc.)
     * @param maxPixelError
     *            maximum tolerable screen space error in pixels (in the rendered image)
     * @param screenX
     *            number of pixels in x direction
     * @param screenY
     *            number of pixels in y direction
     */
    public ViewFrustumCrit( Frustum viewVolume, float maxPixelError, int screenX, int screenY ) {
        this.pixelError = maxPixelError;
        this.screenX = screenX;
        this.screenY = screenY;
        this.viewRegion = viewVolume;
    }

    /**
     * Returns true, iff the region associated with the arc is inside the view frustum volume and the estimated screen
     * projection error is greater than the maximum tolerable error.
     * 
     * @param arc
     *            arc to be checked
     * @return true, iff the arc's region is inside the view frustum and the estimated screen projection error is
     *         greater than the maximum tolerable error
     */
    @Override
    public boolean needsRefinement( Arc arc ) {

        // step 1: only refine a region if it's inside the view volume
        float[][] nodeBBox = arc.getBBox();
        if ( !viewRegion.intersects( arc.getBBox() ) ) {
            return false;
        }

        // step 2: only refine if the region currently violates the screen-space constraint
        float[] eyePos = new float[3];
        eyePos[0] = viewRegion.getEyePos().x;
        eyePos[1] = viewRegion.getEyePos().y;
        eyePos[2] = viewRegion.getEyePos().z;
        float dist = distance.getMinDistance( nodeBBox, eyePos );
        float projectionFactor = estimatePixelSizeForSpaceUnit( dist );
        float maxEdgeLen = pixelError * 1.0f;
        float edgeLen = getEdgeLen( arc ) * projectionFactor;

        LOG.debug( "Checking region (DAG arc) for refinement. Arc error=" + arc.getGeometricError() + ", distance="
                   + dist + ", projectionFactor=" + projectionFactor );
        LOG.debug( "Max acceptable edge length (pixels)=" + maxEdgeLen + ", estimated edge length=" + edgeLen );
        return edgeLen > maxEdgeLen;
    }

    private static final float SQR_2 = (float) Math.sqrt( 2 );

    private float getEdgeLen( Arc arc ) {

        float error = arc.getGeometricError();

        if ( error % 1 == 0 ) {
            return (float) Math.pow( 2, error / 2 );
        }
        return (float) Math.pow( 2, ( error - 1 ) / 2 ) * SQR_2;
    }

    /**
     * Returns a guaranteed upper bound for the size that a world-space unit (e.g. a line with length 1) has in pixels
     * after perspective projection, i.e. in pixels on the screen.
     * 
     * @param dist
     *            distance of the object (from the point-of-view)
     * @return maximum number of pixels that an object of size 1 will cover
     */
    private float estimatePixelSizeForSpaceUnit( float dist ) {
        float h = 2.0f * dist * (float) Math.tan( Math.toRadians( viewRegion.getFOVY() * 0.5f ) );
        return screenY / h;
    }

    @Override
    public String toString() {
        return "{screenX = " + screenX + ", screenY=" + screenY + "}";
    }

    /**
     * Inner class for calculating the minimum distance between a box and a point.
     */
    class BoxPointDistance {

        private float xMin;

        private float yMin;

        private float zMin;

        private float xMax;

        private float yMax;

        private float zMax;

        private float pX;

        private float pY;

        private float pZ;

        /**
         * Returns the minimum distance between the given box and the given point.
         * 
         * @param bbox
         *            box
         * @param p
         *            point
         * @return minimum distance between the given box and the given point
         */
        float getMinDistance( float[][] bbox, float[] p ) {
            xMin = bbox[0][0];
            yMin = bbox[0][1];
            zMin = bbox[0][2];
            xMax = bbox[1][0];
            yMax = bbox[1][1];
            zMax = bbox[1][2];

            pX = p[0];
            pY = p[1];
            pZ = p[2];

            if ( pX >= xMin && pX <= xMax && pY >= yMin && pY <= yMax && pZ >= zMin && pZ <= zMax ) {
                return 0.0f;
            }

            float[] sideDistances = new float[6];
            sideDistances[0] = distanceToFront();
            sideDistances[1] = distanceToBack();
            sideDistances[2] = distanceToLeft();
            sideDistances[3] = distanceToRight();
            sideDistances[4] = distanceToTop();
            sideDistances[5] = distanceToBottom();

            float dist = sideDistances[0];
            for ( int i = 1; i < sideDistances.length; i++ ) {
                if ( sideDistances[i] < dist ) {
                    dist = sideDistances[i];
                }
            }

            return dist;
        }

        private float distanceToFront() {
            float qX = getClipped( pX, xMin, xMax );
            float qY = getClipped( pY, yMin, yMax );
            float qZ = zMax;
            return getDistance( qX, qY, qZ );
        }

        private float distanceToBack() {
            float qX = getClipped( pX, xMin, xMax );
            float qY = getClipped( pY, yMin, yMax );
            float qZ = zMin;
            return getDistance( qX, qY, qZ );
        }

        private float distanceToTop() {
            float qX = getClipped( pX, xMin, xMax );
            float qY = yMax;
            float qZ = getClipped( pZ, zMin, zMax );
            return getDistance( qX, qY, qZ );
        }

        private float distanceToBottom() {
            float qX = getClipped( pX, xMin, xMax );
            float qY = yMin;
            float qZ = getClipped( pZ, zMin, zMax );
            return getDistance( qX, qY, qZ );
        }

        private float distanceToLeft() {
            float qX = xMin;
            float qY = getClipped( pY, yMin, yMax );
            float qZ = getClipped( pZ, zMin, zMax );
            return getDistance( qX, qY, qZ );
        }

        private float distanceToRight() {
            float qX = xMax;
            float qY = getClipped( pY, yMin, yMax );
            float qZ = getClipped( pZ, zMin, zMax );
            return getDistance( qX, qY, qZ );
        }

        private float getDistance( float qX, float qY, float qZ ) {
            float dX = qX - pX;
            float dY = qY - pY;
            float dZ = qZ - pZ;
            return (float) Math.sqrt( dX * dX + dY * dY + dZ * dZ );
        }

        private float getClipped( float value, float min, float max ) {
            if ( value > max ) {
                return max;
            }
            if ( value < min ) {
                return min;
            }
            return value;
        }
    }
}
