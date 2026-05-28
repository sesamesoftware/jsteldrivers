package com.relationaljunction.jdbc.common.h2.sql;

import java.util.*;
import java.io.*;



public class SQLNode {
   // Variables
   String name, type;
   Vector properties; // String
   Hashtable prop_hash;
   Vector childs; //  SQLNode
   Vector ext_obj_names; // )
   Hashtable ext_obj_hash;

   // Methods
   public SQLNode(String name) {
      this.name = name;
      this.type = "";
   }

   public SQLNode(String name, String type) {
      this.name = name;
      this.type = type;
   }

   public SQLNode(String name, String type, Vector new_childs) {
      this.name = name;
      this.type = type;
      this.childs = new Vector();
      childs.add(new_childs);
   }

   public String getName() {
      return this.name;
   }

   public void setName(String new_name) {
      this.name = new_name;
   }

   public String getType() {
      return this.type;
   }

   public void setType(String new_type) {
      this.type = new_type;
   }

   public boolean isSelectQuery() {
      return getName().equals("SELECT_QUERY");
   }

   public boolean isInsert() {
      return getName().equals("INSERT_QUERY");
   }

   public boolean isUpdate() {
      return getName().equals("UPDATE_QUERY");
   }

   public boolean isDelete() {
      return getName().equals("DELETE_QUERY");
   }

   public boolean isCreateTable() {
      return getName().equals("CREATE_QUERY") && getProperty("TYPE").equals("TABLE");
   }

   public boolean isCreateIndex() {
      return getName().equals("CREATE_QUERY") && getProperty("TYPE").equals("INDEX");
   }

   public boolean isCreateView() {
      return getName().equals("CREATE_QUERY") && getProperty("TYPE").equals("VIEW");
   }

   public boolean isDropTable() {
      return getName().equals("DROP_QUERY") && getProperty("TYPE").equals("TABLE") &&
              !getProperty("FROM_CACHE").equalsIgnoreCase("true");
   }

   public boolean isDropTableFromCache() {
      return getName().equals("DROP_QUERY") && getProperty("TYPE").equals("TABLE") &&
              getProperty("FROM_CACHE").equalsIgnoreCase("true");
   }

   public boolean isReloadTable() {
      return getName().equals("RELOAD_TABLE");
   }

   public boolean isReloadCache() {
      return getName().equals("RELOAD_CACHE");
   }

   public boolean isShutdown() {
      return getName().equals("SHUTDOWN");
   }

   public boolean isLockDatabase() {
      return getName().equals("LOCK_DATABASE");
   }

   public boolean isUnlockDatabase() {
      return getName().equals("UNLOCK_DATABASE");
   }

   public boolean isDropView() {
      return getName().equals("DROP_QUERY") && getProperty("TYPE").equals("VIEW");
   }

   public boolean isSave() {
      return getName().equals("SAVE_QUERY");
   }

   public boolean isExplain() {
      return getName().equals("EXPLAIN_QUERY");
   }

   public void setProperty(String prop_name, String prop_value) {
      if (this.properties == null) {
         this.properties = new Vector();
         this.prop_hash = new Hashtable();
      }
      if (!this.properties.contains(prop_name)) this.properties.add(prop_name);
      this.prop_hash.put(prop_name, prop_value);
   }

   public String getProperty(String name) { // ���������� value ("", ���� ������ �������� ���)
      if (this.properties == null) return "";
      String value = (String) this.prop_hash.get(name);
      if (value == null) return "";
      return value;
   }

   public void removeProperty(String prop_name) {
      if (this.properties == null) return;
      this.properties.remove(prop_name);
      this.prop_hash.remove(prop_name);
   }

   public boolean hasProperty(String name) {
      if (this.properties == null) return false;
      String value = (String) this.prop_hash.get(name);
       return (value != null) && (!value.isEmpty());
   }

   public boolean hasChilds() {
      return this.childs != null;
   }

   public void addChild(SQLNode new_child) {
      if (this.childs == null) this.childs = new Vector();
      this.childs.add(new_child);
   }

   public void addChilds(Vector new_childs) {
      if (this.childs == null) this.childs = new Vector();
      this.childs.addAll(new_childs);
   }

   /*SQLNode getOneChild(String node_name) {
  Iterator it=this.childs.iterator();
  while(it.hasNext()) {
    SQLNode node=(SQLNode)it.next();
    if(node.getName().equals(node_name))return node;
  }
  return null;
   }*/

   // return all childs with the corresponding name
   public List<SQLNode> getChildsWithName(String node_name) {
      if (this.childs == null) return new ArrayList<SQLNode>();

      List<SQLNode> result = new LinkedList<SQLNode>();

      for (Object child : this.childs) {
         SQLNode node = (SQLNode) child;
         if (node.getName().equals(node_name)) result.add(node);
      }
      return result;
   }

   public Vector getChildsWithType(String node_type) {
      if (this.childs == null) return new Vector();
      Vector ret_vec = new Vector();
      Iterator it = this.childs.iterator();
      while (it.hasNext()) {
         SQLNode node = (SQLNode) it.next();
         if (node.getType().equals(node_type)) ret_vec.add(node);
      }
      return ret_vec;
   }

   // return the first child with the corresponding name
   public SQLNode getUniqueChildWithName(String node_name) {
      if (this.childs == null) return null;
      Iterator it = this.childs.iterator();
      while (it.hasNext()) {
         SQLNode node = (SQLNode) it.next();
         if (node.getName().equals(node_name)) return node;
      }
      return null;
   }

   public SQLNode getUniqueChildWithType(String node_type) {
      if (this.childs == null) return null;
      Iterator it = this.childs.iterator();
      while (it.hasNext()) {
         SQLNode node = (SQLNode) it.next();
         if (node.getType().equals(node_type)) return node;
      }
      return null;
   }

   public void removeChild(SQLNode child) {
      this.childs.remove(child);
   }

   public Vector getAllChilds() {
      if (this.childs == null) return new Vector();
      return this.childs;
   }

   // ������, ��������� � ������������ ������.
   public String getNodeInfo() {
      return this.name + "(" + this.type + ")";
   }

   public String getPropertiesInfo(String capt) {
      StringBuilder out_str = new StringBuilder();
      Iterator it = this.properties.iterator();
      while (it.hasNext()) {
         String prop_name = (String) it.next();
         String prop_val = (String) this.prop_hash.get(prop_name);
         out_str.append(capt).append("(property) ").append(prop_name).append(" = ").append(prop_val).append("\r\n");
      }
      return out_str.toString();
   }

   public Vector getAllPropertyNames() {
      return this.properties;
   }

   public String getNodeInfoWithChilds(String capt) {
      StringBuilder out_str = new StringBuilder(capt + this.getNodeInfo() + "\r\n");
      if (this.properties != null)
         out_str.append(this.getPropertiesInfo(capt + "  "));
      if (this.hasChilds()) {
         Iterator it = this.childs.iterator();
         while (it.hasNext()) {
            SQLNode child = (SQLNode) it.next();
            out_str.append(child.getNodeInfoWithChilds(capt + "  "));
         }
      }
      return out_str.toString();
   }

   // searches through all nodes with the corresponding name
   public void getNodesByName(String name, List<SQLNode> nodes) {
//    if (this.properties != null)
      if (this.getName().equals(name)) {
         nodes.add(this);
//      System.out.println(this.getPropertiesInfo(""));
      }

      if (this.hasChilds()) {
         Iterator it = this.childs.iterator();
         while (it.hasNext()) {
            SQLNode child = (SQLNode) it.next();
            child.getNodesByName(name, nodes);
         }
      }
   }

   public void printTreeInfo() {
      this.printTreeInfo(System.out);
   }

   public void printTreeInfo(PrintStream ps) {
      ps.println(this.getNodeInfoWithChilds(""));
   }

   public String getTreeInfo() {
      return this.getNodeInfoWithChilds("");
   }

   public void setExternalObject(String obj_name, Object obj) {
      if (this.ext_obj_names == null) {
         this.ext_obj_names = new Vector();
         this.ext_obj_hash = new Hashtable();
      }
      if (!this.ext_obj_names.contains(obj_name)) this.ext_obj_names.add(obj_name);
      this.ext_obj_hash.put(obj_name, obj);
   }

   public Object getExternalObject(String obj_name) { // ���������� value ("", ���� ������ �������� ���)
      if (this.ext_obj_names == null) return null;
      return this.ext_obj_hash.get(obj_name);
   }

   public void removeExternalObject(String obj_name) {
      this.ext_obj_names.remove(obj_name);
      this.ext_obj_hash.remove(obj_name);
   }

   public boolean hasExternalObject(String obj_name) {
      return this.getExternalObject(obj_name) != null;
   }
}
