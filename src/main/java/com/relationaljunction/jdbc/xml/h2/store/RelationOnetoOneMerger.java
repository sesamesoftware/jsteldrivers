package com.relationaljunction.jdbc.xml.h2.store;

import java.util.*;

public class RelationOnetoOneMerger<T> implements Iterator<List<T>>, Iterable<List<T>> {
   private List<List<T>> arrays = new LinkedList<List<T>>();
   private int maxArraySize = 0;
   private T nullElement = null;
   private int currentIndex = 0;
   private boolean repeatValue = true;
   private boolean returnLastValueOnly = false;

   public RelationOnetoOneMerger() {
   }

   public RelationOnetoOneMerger(List<List<T>> arrays) {
      setArrays(arrays);
   }

   public void addArray(List<T> list) {
      arrays.add(list);

      if (list.size() > maxArraySize) maxArraySize = list.size();
   }

   public void addArray(T[] array) {
      addArray(Arrays.asList(array));
   }

   public void setArrays(List<List<T>> arrays) {
      currentIndex = 0;

      this.arrays = arrays;

      for (List<T> array : arrays) {
         if (array.size() > maxArraySize) maxArraySize = array.size();
      }
   }

   public void setNullElement(T nullElement) {
      this.nullElement = nullElement;
   }

   public boolean hasNext() {
      if (returnLastValueOnly) {
         return maxArraySize > 0 && currentIndex < 1;
      } else {
         return currentIndex < maxArraySize;
      }
   }

   public List<T> next() {
      List<T> result = new ArrayList<T>(arrays.size());

//      for (List<T> array : arrays) {
//         if (currentIndex < array.size()) {
//            result.add(array.get(currentIndex));
//         } else {
//            result.add(nullElement);
//         }
//      }

      if (returnLastValueOnly) {
         // return the latest values only
         for (List<T> array : arrays) {
            if (!array.isEmpty()) {
               // add the latest element
               result.add(array.get(array.size() - 1));
            } else {
               // add NULL element
               result.add(nullElement);
            }
         }
      } else {
         for (List<T> array : arrays) {
            if (currentIndex < array.size()) {
               result.add(array.get(currentIndex));
            } else if (repeatValue && !array.isEmpty()) {
               // repeat the latest value in an array
               result.add(array.get(array.size() - 1));
            } else {
               // add NULL element
               result.add(nullElement);
            }
         }
      }

      currentIndex++;

      return result;
   }

   public void remove() {
   }

   public void clear() {
      currentIndex = 0;
      maxArraySize = 0;
      arrays.clear();
   }

   public Iterator<List<T>> iterator() {
      currentIndex = 0;
      return this;
   }

   public void setRepeatLastValue(boolean repeatValue) {
      this.repeatValue = repeatValue;
   }

   public void setReturnLastValueOnly(boolean returnLastValueOnly) {
      this.returnLastValueOnly = returnLastValueOnly;
   }

   public static void main(String[] args) {
      String[] a = {"a0", "a1", "a2"};
      String[] b = {"b0", "b1", "b2"};
      String[] c = {"c0", "c1", "c2", "c3"};

      RelationOnetoOneMerger<String> rm = new RelationOnetoOneMerger<String>();
      rm.setNullElement("<null>");
      rm.addArray(a);
      rm.addArray(b);
      rm.addArray(c);

      for (List<String> resultArray : rm) {
         System.out.println(resultArray);
      }
   }
}
