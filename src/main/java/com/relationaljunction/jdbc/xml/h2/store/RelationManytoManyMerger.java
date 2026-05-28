package com.relationaljunction.jdbc.xml.h2.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RelationManytoManyMerger<T> {
   private final List<T> elements = new ArrayList<T>();
   private boolean repeated = false;
   private int curIndex = 0;
   private RelationManytoManyMerger<T> nextCartesianProductList = null;

   public RelationManytoManyMerger(List<List<T>> arrays) {
   }

   public RelationManytoManyMerger(T[] elementsArray) {
      Collections.addAll(elements, elementsArray);
   }

   public void addNextCartesianProductList(RelationManytoManyMerger<T> nextVectorIterator) {
      this.nextCartesianProductList = nextVectorIterator;
   }

   public T getCurrentElement() {
      return elements.get(curIndex);
   }

   public boolean isRepeated() {
      return repeated;
   }

   public void moveToNextElement() {
      curIndex++;
      if (curIndex == elements.size()) {
         curIndex = 0;
         repeated = true;
      }
   }

   public boolean next(List<T> l) {
      l.add(getCurrentElement());

      boolean nextIteration = true;
      if (nextCartesianProductList != null)
         nextIteration = nextCartesianProductList.next(l);

      if (nextIteration) {
         moveToNextElement();
         if (isRepeated()) {
            repeated = false;
            return true;
         }
      }

      return false;
   }

   public static void main(String[] args) {
      String[] a = {"a0"};
      String[] b = {"b0"};
      String[] c = {"c0"};
      String[] d = {"d0", "d1"};

      RelationManytoManyMerger<String> vIta = new RelationManytoManyMerger<String>(a);
      RelationManytoManyMerger<String> vItb = new RelationManytoManyMerger<String>(b);
      RelationManytoManyMerger<String> vItc = new RelationManytoManyMerger<String>(c);
      RelationManytoManyMerger<String> vItd = new RelationManytoManyMerger<String>(d);
      vIta.addNextCartesianProductList(vItb);
      vItb.addNextCartesianProductList(vItc);
      vItc.addNextCartesianProductList(vItd);

      boolean hasMoreElements = true;

      do {
         ArrayList<String> row = new ArrayList<String>();
         hasMoreElements = vIta.next(row);
         System.out.println(row);
      } while (!hasMoreElements);
   }
}
