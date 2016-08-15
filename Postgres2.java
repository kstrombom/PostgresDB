/*
Author: Kate Strombom
RULES:
  1)Don't have periods in your fileName unless there is an extension
  2)This program assumes the first line in a csv type file is a header and ignores it
  3)Upserts based on unique code
*/
import java.sql.*;
import java.util.*;
import java.io.*;

import com.opencsv.CSVReader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;


public class Postgres2 {
   public static void main( String args[] )
     {
       Connection connection = null;
       Statement stmt = null;

       String fileName = "";
       String tableName = "";

       //connect to database
       try
       {
         Class.forName("org.postgresql.Driver");
         connection = DriverManager
            .getConnection("jdbc:postgresql://localhost:5432/TestDB?sslmode=require",
            "username", "password");
        connection.setAutoCommit(false);
       }
       catch ( Exception e )
       {
         System.err.println( e.getClass().getName()+": "+ e.getMessage() );
         System.exit(0);
       }
       System.out.println("Connected to TestDB database");
       //end connect to database

       //Grab and work with all arguments within command line
       for(int i = 0; i < args.length; i++)
       {
         //Seperate fileName and table name from path
         int index = args[i].lastIndexOf("/");
         tableName = args[i].substring(index+1);
         int index2 = tableName.lastIndexOf('.');
         if(index2 > 0)
         {
          tableName = tableName.substring(0, index2);
         }
         tableName = tableName.toLowerCase();
         fileName = args[i];

         try
         {
           /*CREATE TABLE*/
           //make create table statement and upload to database
            stmt = connection.createStatement();

            String createTable = "CREATE TABLE IF NOT EXISTS " + tableName +
                         "(id SERIAL PRIMARY KEY," +
                         " oscre_code VARCHAR(8) NOT NULL UNIQUE, " +
                         " internal_app_code VARCHAR(20) NOT NULL UNIQUE, " +
                         " ui_display_value VARCHAR(100)  NOT NULL, " +
                         " ui_display_order INT NOT NULL, " +
                         " description VARCHAR(500), " +
                         " created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);";
            stmt.executeUpdate(createTable);
            /*CREATE TABLE*/
         }
         catch ( Exception e )
         {
           System.err.println( e.getClass().getName()+": "+ e.getMessage() );
           try
           {
             if(connection != null)
             {
               connection.rollback();
               connection.close();
               System.out.println("Connection to TestDB has been closed");
             }
           }
           catch(Exception d)
           {
             System.err.println( d.getClass().getName()+": "+ d.getMessage() );
             System.exit(0);
           }

           System.exit(0);
         }

         try
         {
           /*READ AND UPSERT CSV DATA*/
           CSVReader reader = new CSVReader(new FileReader(fileName));
           String[] nextLine;

           String oscreCode = "";
           String internalAppCode = "";
           int uiDisplayOrder = 0;
           String uiDisplayValue = "";
           String description ="";

           int l = 0;

           while((nextLine = reader.readNext()) != null)
           {
             if(l > 0) //skips over the header info in a CSV
             {
               oscreCode = nextLine[0];
               internalAppCode = nextLine[1];
               uiDisplayValue = nextLine[2];
               uiDisplayOrder = Integer.parseInt(nextLine[3]);
               description = nextLine[4];

               String insert = "INSERT INTO " + tableName + " (oscre_code, internal_app_code, ui_display_value, ui_display_order, description) VALUES" +
                               " ('" + oscreCode + "', '" + internalAppCode + "', '" + uiDisplayValue + "', " + uiDisplayOrder + ", '" + description + "')" +
                               " ON CONFLICT (oscre_code) DO UPDATE SET oscre_code = '" +
                               oscreCode + "', internal_app_code = '" + internalAppCode + "', ui_display_value = '" + uiDisplayValue + "', ui_display_order = " + uiDisplayOrder +
                               ", description = '" + description + "', created_at = DEFAULT;";
               stmt.executeUpdate(insert);
             }
             l++;
           }
           stmt.close();
           /*READ AND UPSERT CSV DATA*/
         }
         catch ( Exception e )
         {
           System.err.println( e.getClass().getName()+": "+ e.getMessage() );
           try
           {
             if(connection != null)
             {
               connection.rollback();
               connection.close();
               System.out.println("Connection to TestDB has been closed");
             }
           }
           catch(Exception d)
           {
             System.err.println( d.getClass().getName()+": "+ d.getMessage() );
             System.exit(0);
           }
           System.exit(0);
         }

         System.out.println(args[i] + " has been uploaded");

       }

       //close connection to database
       try
       {
         connection.commit();
         connection.close();
       }
       catch ( Exception e )
       {
         System.err.println( e.getClass().getName()+": "+ e.getMessage() );
         System.exit(0);
       }
       System.out.println("Connection to TestDB closed");
     }
}
