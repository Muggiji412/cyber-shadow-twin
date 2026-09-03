import java.sql.*;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        String url = "jdbc:mysql://localhost:3306/shadow_twin_db";
        String user = "root";
        String password = "Muggi@123";

        try {

            Connection con =
                    DriverManager.getConnection(url,user,password);

            while(true){

                System.out.println("\n===== SHADOW TWIN =====");
                System.out.println("1. Add User");
                System.out.println("2. View Users");
                System.out.println("3. Save Memory");
                System.out.println("4. View Memories");
                System.out.println("5. Search Memories");
                System.out.println("6. Talk to Shadow Twin");
                System.out.println("7. View Chat History");
                System.out.println("8. Exit");

                System.out.print("Choose: ");

                int choice=sc.nextInt();
                sc.nextLine();

                Statement st=
                        con.createStatement();

                if(choice==1){

                    System.out.print("Username: ");
                    String username=sc.nextLine();

                    System.out.print("Email: ");
                    String email=sc.nextLine();

                    String sql =
                            "INSERT INTO users(username,email) VALUES (?,?)";

                    PreparedStatement pst =
                            con.prepareStatement(sql);

                    pst.setString(1,username);
                    pst.setString(2,email);

                    pst.executeUpdate();

                    System.out.println("User Added!");
                }

                else if(choice==2){

                    ResultSet rs=
                            st.executeQuery("SELECT * FROM users");

                    while(rs.next()){

                        System.out.println(
                                rs.getInt("id")+" | "+
                                        rs.getString("username")+" | "+
                                        rs.getString("email")
                        );
                    }
                }

                else if(choice==3){

                    System.out.print("Enter memory: ");

                    String memory=sc.nextLine();

                    String sql=
                            "INSERT INTO memories(memory_text) VALUES(?)";

                    PreparedStatement pst=
                            con.prepareStatement(sql);

                    pst.setString(1,memory);

                    pst.executeUpdate();

                    System.out.println("Memory Saved!");
                }

                else if(choice==4){

                    ResultSet rs=
                            st.executeQuery(
                                    "SELECT * FROM memories");

                    while(rs.next()){

                        System.out.println(
                                rs.getInt("id")+" | "+
                                        rs.getString("memory_text")
                        );
                    }
                }
                else if(choice==5){

                    System.out.print("Enter keyword: ");
                    String keyword=sc.nextLine();

                    ResultSet rs=
                            st.executeQuery(
                                    "SELECT * FROM memories WHERE memory_text LIKE '%"
                                            +keyword+"%'"
                            );

                    while(rs.next()){

                        System.out.println(
                                rs.getInt("id")+" | "+
                                        rs.getString("memory_text")
                        );
                    }
                }
                else if(choice==6){

                    System.out.print("You: ");
                    String message=sc.nextLine();

                    message=message.toLowerCase();

                    String reply="";

                    if(message.contains("my name is")){

                        PreparedStatement pst=
                                con.prepareStatement(
                                        "INSERT INTO memories(memory_text) VALUES(?)");

                        pst.setString(1,message);
                        pst.executeUpdate();

                        reply="Nice to meet you!";
                    }

                    else if(message.contains("what is my name")){

                        ResultSet rs=
                                st.executeQuery(
                                        "SELECT memory_text FROM memories WHERE memory_text LIKE 'my name is%'");

                        if(rs.next()){

                            String name=
                                    rs.getString("memory_text");

                            name=
                                    name.replace("my name is ","");

                            reply=
                                    "Your name is "+name;
                        }

                        else{

                            reply=
                                    "I don't know your name yet.";
                        }
                    }

                    // IMPORTANT: ask BEFORE save
                    else if(message.contains("what is my favorite")){

                        String category=
                                message.replace(
                                        "what is my favorite ","");

                        ResultSet rs=
                                st.executeQuery(
                                        "SELECT value_text FROM preferences WHERE category='"
                                                +category+"'");

                        if(rs.next()){

                            reply=
                                    "Your favorite "
                                            +category+
                                            " is "
                                            +rs.getString("value_text");
                        }

                        else{

                            reply=
                                    "I don't know that yet.";
                        }
                    }

                    else if(message.contains("my favorite")){

                        String data=
                                message.replace(
                                        "my favorite ","");

                        String parts[]=
                                data.split(" is ");

                        if(parts.length==2){

                            String category=
                                    parts[0];

                            String value=
                                    parts[1];

                            PreparedStatement pst=
                                    con.prepareStatement(
                                            "INSERT INTO preferences(category,value_text) VALUES(?,?)"
                                    );

                            pst.setString(1,category);
                            pst.setString(2,value);

                            pst.executeUpdate();

                            reply=
                                    "Got it. I'll remember your favorite "
                                            +category;
                        }
                    }

                    else if(message.contains("i like")){

                        PreparedStatement pst=
                                con.prepareStatement(
                                        "INSERT INTO memories(memory_text) VALUES(?)"
                                );

                        pst.setString(1,message);
                        pst.executeUpdate();

                        reply=
                                "Got it. I'll remember that.";
                    }

                    else if(message.contains("what do i like")){

                        ResultSet rs=
                                st.executeQuery(
                                        "SELECT memory_text FROM memories WHERE memory_text LIKE '%i like%'"
                                );

                        reply="You told me:\n";

                        while(rs.next()){

                            reply+=
                                    "- "+
                                            rs.getString("memory_text")
                                            +"\n";
                        }
                    }

                    else if(message.contains("what do you know about me")){

                        reply="Here's what I know:\n";

                        ResultSet rs1=
                                st.executeQuery(
                                        "SELECT memory_text FROM memories"
                                );

                        while(rs1.next()){

                            reply +=
                                    "- "+
                                            rs1.getString("memory_text")
                                            +"\n";
                        }

                        ResultSet rs2=
                                st.executeQuery(
                                        "SELECT category,value_text FROM preferences"
                                );

                        while(rs2.next()){

                            reply +=
                                    "- Favorite "
                                            +rs2.getString("category")
                                            +" : "
                                            +rs2.getString("value_text")
                                            +"\n";
                        }
                    }
                    else if(message.contains("good morning")){

                        ResultSet rs=
                                st.executeQuery(
                                        "SELECT memory_text FROM memories WHERE memory_text LIKE 'my name is%'"
                                );

                        if(rs.next()){

                            String name=
                                    rs.getString("memory_text");

                            name=
                                    name.replace("my name is ","");

                            reply=
                                    "Good morning "+name+" ☀️";
                        }

                        else{

                            reply=
                                    "Good morning ☀️";
                        }
                    }

                    else if(message.contains("good night")){

                        ResultSet rs=
                                st.executeQuery(
                                        "SELECT memory_text FROM memories WHERE memory_text LIKE 'my name is%'"
                                );

                        if(rs.next()){

                            String name=
                                    rs.getString("memory_text");

                            name=
                                    name.replace("my name is ","");

                            reply=
                                    "Good night "+name+" 🌙";
                        }

                        else{

                            reply=
                                    "Good night 🌙";
                        }
                    }
                    else{

                        reply=
                                "Interesting... tell me more.";
                    }

                    System.out.println(
                            "Shadow Twin: "+reply
                    );

                    PreparedStatement chatpst=
                            con.prepareStatement(
                                    "INSERT INTO chats(user_message,twin_reply) VALUES(?,?)"
                            );

                    chatpst.setString(1,message);
                    chatpst.setString(2,reply);

                    chatpst.executeUpdate();
                }
                else if(choice==7){

                    ResultSet rs=
                            st.executeQuery(
                                    "SELECT * FROM chats"
                            );

                    while(rs.next()){

                        System.out.println(
                                "You: "
                                        +rs.getString("user_message")
                        );

                        System.out.println(
                                "Shadow Twin: "
                                        +rs.getString("twin_reply")
                        );

                        System.out.println("----------------");
                    }
                }
                else if(choice==8){

                    System.out.println(
                            "Shadow Twin shutting down...");
                    break;
                }

            }

            con.close();

        }

        catch(Exception e){
            System.out.println(e);
        }

    }

}