/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.miloss.kmlreducer;

import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author alex
 */
public class Main {

     public static void main(String[] args) throws Exception {

          String folder = args[0];
          File parent = new File(folder);
          File[] listFiles = parent.listFiles(new FileFilter() {
               @Override
               public boolean accept(File pathname) {
                    if (pathname.toString().toLowerCase().endsWith(".kml")) {
                         return true;
                    }
                    return false;
               }
          });
          int skip=0;
          List<Container> errors = new ArrayList<Container>();
          for (int x = 0; x < listFiles.length; x++) {
               if (new File(listFiles[x].getAbsolutePath() + "reduced.kml").exists()){
                    System.err.println(listFiles[x].getAbsolutePath() + "reduced.kml exists already, skipping" );
                    skip++;
                    continue;
               }
               Kml kml = new Kml();
               Kml unmarshal = Kml.unmarshal(listFiles[x]);

               try {
                    Document feature = (Document) unmarshal.getFeature();
                    for (int i = 0; i < feature.getFeature().size(); i++) {
                         Folder get = (Folder) feature.getFeature().get(i);
                         for (int k = 0; k < get.getFeature().size(); k++) {
                              Placemark get1 = (Placemark) get.getFeature().get(k);
                              //System.out.println(get1.getName());
                              if (get1.getGeometry() instanceof Polygon) {
                                   Polygon polygon = (Polygon) get1.getGeometry();
                                   LinearRing ring = (LinearRing) polygon.getOuterBoundaryIs().getLinearRing();
                                   List<Coordinate> coordinates = ring.getCoordinates();
                                   List<Coordinate> reduceWithTolerance = reduceWithTolerance(ring.getCoordinates(), 15.0);
                                   System.out.println(coordinates.size() + " reduced to " + reduceWithTolerance.size());
                                   ring.setCoordinates(reduceWithTolerance);
                              }
                         }
                    }

                    unmarshal.marshal(new File(listFiles[x].getAbsolutePath() + "reduced.kml"));
               } catch (Exception ex) {
                    System.out.println("unable to process " + listFiles[x].getAbsolutePath());
                    Container c = new Container();
                    c.ex = ex;
                    c.path = listFiles[x];
                    errors.add(c);
               }
          }
          for (int x = 0; x < errors.size(); x++) {
               System.out.println("unable to process " + errors.get(x).path.getAbsolutePath());
               errors.get(x).ex.printStackTrace();
          }
          System.out.println(skip + " files skipped");

     }

     static class Container {

          public File path;
          public Exception ex;
     }

     /**
      * Reduce the number of points in a shape using the Douglas-Peucker
      * algorithm
      *
      * @param	shape The shape to reduce
      * @param tolerance The tolerance to decide whether or not to keep a point,
      * in the coordinate system of the points (micro-degrees here)
      * @return the reduced shape
      */
     public static List<Coordinate> reduceWithTolerance(List<Coordinate> shape, double tolerance) {
          int n = shape.size();
          // if a shape has 2 or less points it cannot be reduced
          if (tolerance <= 0 || n < 3) {
               return shape;
          }

          boolean[] marked = new boolean[n]; //vertex indexes to keep will be marked as "true"
          for (int i = 1; i < n - 1; i++) {
               marked[i] = false;
          }
          // automatically add the first and last point to the returned shape
          marked[0] = marked[n - 1] = true;

          // the first and last points in the original shape are
          // used as the entry point to the algorithm.
          douglasPeuckerReduction(
               shape, // original shape
               marked, // reduced shape
               tolerance, // tolerance
               0, // index of first point
               n - 1 // index of last point
          );

          // all done, return the reduced shape
          ArrayList<Coordinate> newShape = new ArrayList<Coordinate>(n); // the new shape to return
          for (int i = 0; i < n; i++) {
               if (marked[i]) {
                    newShape.add(shape.get(i));
               }
          }
          return newShape;
     }

     /**
      * Reduce the points in shape between the specified first and last index.
      * Mark the points to keep in marked[]
      *
      * @param shape	The original shape
      * @param marked	The points to keep (marked as true)
      * @param tolerance The tolerance to determine if a point is kept
      * @param firstIdx The index in original shape's point of the starting
      * point for this line segment
      * @param lastIdx The index in original shape's point of the ending point
      * for this line segment
      */
     private static void douglasPeuckerReduction(List<Coordinate> shape, boolean[] marked, double tolerance, int firstIdx, int lastIdx) {
          if (lastIdx <= firstIdx + 1) {
               // overlapping indexes, just return
               return;
          }

          // loop over the points between the first and last points
          // and find the point that is the farthest away
          double maxDistance = 0.0;
          int indexFarthest = 0;

          Coordinate firstPoint = shape.get(firstIdx);
          Coordinate lastPoint = shape.get(lastIdx);

          for (int idx = firstIdx + 1; idx < lastIdx; idx++) {
               Coordinate point = shape.get(idx);

               double distance = orthogonalDistance(point, firstPoint, lastPoint);

               // keep the point with the greatest distance
               if (distance > maxDistance) {
                    maxDistance = distance;
                    indexFarthest = idx;
               }
          }

          if (maxDistance > tolerance) {
               //The farthest point is outside the tolerance: it is marked and the algorithm continues. 
               marked[indexFarthest] = true;

               // reduce the shape between the starting point to newly found point
               douglasPeuckerReduction(shape, marked, tolerance, firstIdx, indexFarthest);

               // reduce the shape between the newly found point and the finishing point
               douglasPeuckerReduction(shape, marked, tolerance, indexFarthest, lastIdx);
          }
          //else: the farthest point is within the tolerance, the whole segment is discarded.
     }

     /**
      * Calculate the orthogonal distance from the line joining the lineStart
      * and lineEnd points to point
      *
      * @param point The point the distance is being calculated for
      * @param lineStart The point that starts the line
      * @param lineEnd The point that ends the line
      * @return The distance in points coordinate system
      */
     public static double orthogonalDistance(Coordinate point, Coordinate lineStart, Coordinate lineEnd) {
          double area = Math.abs(
               (1.0 * lineStart.getLatitude() * 1e6 * lineEnd.getLongitude() * 1e6
               + 1.0 * lineEnd.getLatitude() * 1e6 * point.getLongitude() * 1e6
               + 1.0 * point.getLatitude() * 1e6 * lineStart.getLongitude() * 1e6
               - 1.0 * lineEnd.getLatitude() * 1e6 * lineStart.getLongitude() * 1e6
               - 1.0 * point.getLatitude() * 1e6 * lineEnd.getLongitude() * 1e6
               - 1.0 * lineStart.getLatitude() * 1e6 * point.getLongitude() * 1e6) / 2.0
          );

          double bottom = Math.hypot(
               lineStart.getLatitude() * 1e6 - lineEnd.getLatitude() * 1e6,
               lineStart.getLongitude() * 1e6 - lineEnd.getLongitude() * 1e6
          );

          return (area / bottom * 2.0);
     }

}
