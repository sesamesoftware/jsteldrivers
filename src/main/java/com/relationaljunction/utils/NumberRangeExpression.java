package com.relationaljunction.utils;

public class NumberRangeExpression {

   private final NumberRange[] ranges;
   private final int min, max;
   private NumberRange rangeToMaximum;

   /**
    * Create a new {@link NumberExpression}.
    *
    * @param pattern the expression pattern.
    * @throws IllegalArgumentException if the pattern is malformed
    */
   public NumberRangeExpression(String pattern) {
      String[] parts = pattern.toLowerCase().split(",", -1);
      ranges = new NumberRange[parts.length];

      int min = Integer.MAX_VALUE, max = 0;

      for (int i = 0; i < ranges.length; i++) {
         String part = parts[i].trim();
         try {
            if (part.equals("*")) {
               // * (all)
               ranges[i] = new NumberRange(0, Integer.MAX_VALUE, 0, 1);
            } else if (part.matches("\\*/\\d+")) {
               ranges[i] = new NumberRange(0, Integer.MAX_VALUE, 0, Integer.parseInt(part.substring(2)));
            } else if (part.matches("\\d+")) {
               // 57 (number)
               int value = Integer.parseInt(part);
               ranges[i] = new NumberRange(value, value, 0, 1);
            } else if (part.matches("(\\d+)\\+")) {
               // 5+
               if (rangeToMaximum != null) throw new IllegalArgumentException(
                       "Range with all numbers higher then some value (e.g. 10+) must be only one: " + part);

               int value = Integer.parseInt(part.substring(0, part.indexOf("+")));
               ranges[i] = new NumberRange(value, Integer.MAX_VALUE, 0, 1);
               rangeToMaximum = ranges[i];
            } else if (part.matches("\\d*-\\d*")) {
               // 1-10 (range)
               String[] limits = part.split("-", -1);
               int from = limits[0].isEmpty() ? 0 : Integer.parseInt(limits[0]);
               int to = limits[1].isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(limits[1]);
               if (to < from)
                  throw new IllegalArgumentException("Invalid pattern: " + part);
               ranges[i] = new NumberRange(from, to, 0, 1);
            } else if (part.matches("\\d*-\\d*/\\d+")) {
               String[] rangeAndModulus = part.split("/", -1);
               String[] limits = rangeAndModulus[0].split("-", -1);
               int from = limits[0].isEmpty() ? 0 : Integer.parseInt(limits[0]);
               int to = limits[1].isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(limits[1]);
               int modulus = Integer.parseInt(rangeAndModulus[1]);
               if (to < from)
                  throw new IllegalArgumentException("Invalid pattern: " + part);
               ranges[i] = new NumberRange(from, to, from % modulus, modulus);
            } else if (part.matches("\\d*-\\d*[eo]")) {
               String[] limits = part.substring(0, part.length() - 1).split("-", -1);
               int from = limits[0].isEmpty() ? 0 : Integer.parseInt(limits[0]);
               int to = limits[1].isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(limits[1]);
               if (to < from)
                  throw new IllegalArgumentException("Invalid pattern: " + part);
               ranges[i] = new NumberRange(from, to, part.charAt(part.length() - 1) == 'o' ? 1 : 0, 2);
            } else {
               throw new IllegalArgumentException("Invalid pattern: " + part);
            }

            max = Math.max(max, ranges[i].getMax());
            min = Math.min(min, ranges[i].getMin());
         } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid pattern: " + part);
         }
      }
      this.max = max;
      this.min = min;
   }

   /**
    * Check whether this number expression matches the given number.
    *
    * @param number the number to check against
    * @return whether the expression matches the number
    */
   public boolean matches(int number) {
      if (number < min || number > max)
         return false;

      for (NumberRange range : ranges) {
         if (range.matches(number))
            return true;
      }

      return false;
   }

   /**
    * if a number is contained in {number}+ range
    *
    * @param number
    * @return
    */
   public boolean isNumberInRangeToMaximum(int number) {
      return rangeToMaximum != null && rangeToMaximum.matches(number);
   }

   /**
    * Return the minimum number that can be matched.
    */
   public int getMinimum() {
      return min;
   }

   /**
    * Return the maximum number that can be matched.
    */
   public int getMaximum() {
      return max;
   }

   private static class NumberRange {
      private final int min, max, remainder, modulus;

      NumberRange(int min, int max, int remainder, int modulus) {
         this.min = min;
         this.max = max;
         this.remainder = remainder;
         this.modulus = modulus;
      }

      boolean matches(int number) {
         return number >= min && number <= max && number % modulus == remainder;
      }

      int getMin() {
         return min;
      }

      int getMax() {
         return max;
      }
   }

   public static void main(String[] args) {
//      NumberRangeExpression nre = new NumberRangeExpression("1, 2,3,4,6,9,14,15,16,19, 10-30");
//      System.out.println("min: " + nre.getMinimum() + ", max=" + nre.getMaximum());

      NumberRangeExpression nre = new NumberRangeExpression("2, 4-7, 10+, 100+");
      System.out.println("min: " + nre.getMinimum() + ", max=" + nre.getMaximum());

      System.out.println(nre.matches(3));
      System.out.println(nre.matches(4));
      System.out.println(nre.matches(7));
      System.out.println(nre.matches(8));
      System.out.println(nre.matches(57));
      System.out.println(nre.matches(1000));
   }
}
