package com.relationaljunction.jdbc.xml.h2.store;

import java.util.*;

public class CartesianProductList {
  private List elements = new Vector();
  private boolean repeated = false;
  private int curIndex = 0;
  private CartesianProductList nextCartesianProductList = null;

  public CartesianProductList(List elements) {
    this.elements = elements;
  }

  public CartesianProductList(Object[] elementsArray) {
      Collections.addAll(elements, elementsArray);
  }

  public void addNextCartesianProductList(CartesianProductList nextVectorIterator) {
    this.nextCartesianProductList = nextVectorIterator;
  }

  public Object getCurrentElement() {
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

  public boolean next(List l) {
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

    CartesianProductList vIta = new CartesianProductList(a);
    CartesianProductList vItb = new CartesianProductList(b);
    CartesianProductList vItc = new CartesianProductList(c);
    CartesianProductList vItd = new CartesianProductList(d);
    vIta.addNextCartesianProductList(vItb);
    vItb.addNextCartesianProductList(vItc);
    vItc.addNextCartesianProductList(vItd);

    boolean hasMoreElements = true;

    do {
      Vector row = new Vector();
      hasMoreElements = vIta.next(row);
      System.out.println(row);
    } while (!hasMoreElements);
  }
}
