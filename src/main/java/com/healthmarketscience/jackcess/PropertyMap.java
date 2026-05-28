/*
Copyright (c) 2013 James Ahlborn

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
USA
*/

package com.healthmarketscience.jackcess;

import java.io.IOException;

/**
 * Map of properties for a database object.
 *
 * @author James Ahlborn
 * @usage _general_class_
 */
public interface PropertyMap extends Iterable<PropertyMap.Property>
{
  String ACCESS_VERSION_PROP = "AccessVersion";
  String TITLE_PROP = "Title";
  String AUTHOR_PROP = "Author";
  String COMPANY_PROP = "Company";

  String DEFAULT_VALUE_PROP = "DefaultValue";
  String REQUIRED_PROP = "Required";
  String ALLOW_ZERO_LEN_PROP = "AllowZeroLength";
  String DECIMAL_PLACES_PROP = "DecimalPlaces";
  String FORMAT_PROP = "Format";
  String INPUT_MASK_PROP = "InputMask";
  String CAPTION_PROP = "Caption";
  String VALIDATION_RULE_PROP = "ValidationRule";
  String VALIDATION_TEXT_PROP = "ValidationText";
  String GUID_PROP = "GUID";
  String DESCRIPTION_PROP = "Description";


  String getName();

  int getSize();

  boolean isEmpty();

  /**
   * @return the property with the given name, if any
   */
  Property get(String name);

  /**
   * @return the value of the property with the given name, if any
   */
  Object getValue(String name);

  /**
   * @return the value of the property with the given name, if any, otherwise
   *         the given defaultValue
   */
  Object getValue(String name, Object defaultValue);

  /**
   * Creates a new (or updates an existing) property in the map.
   * <p/>
   * Note, this change will not be persisted until the {@link #save} method
   * has been called.
   *
   * @return the newly created (or updated) property
   */
  Property put(String name, DataType type, Object value);

  /**
   * Removes the property with the given name
   *
   * @return the removed property, or {@code null} if none found
   */
  Property remove(String name);

  /**
   * Saves the current state of this map.
   */
  void save() throws IOException;

  /**
   * Info about a property defined in a PropertyMap.
   */
  interface Property
  {
    String getName();

    DataType getType();

    Object getValue();

    /**
     * Sets the new value for this property.
     * <p/>
     * Note, this change will not be persisted until the {@link
     * PropertyMap#save} method has been called.
     */
    void setValue(Object newValue);
  }
}
