package edu.uwm.cs361;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import jdo.Course;
import jdo.Section;
import jdo.User;

import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;

import com.google.appengine.api.blobstore.BlobKey;

/**
 * This class defines the Datastore.
 * @author 5guys
 */
public class Datastore 
{
	private HttpServletRequest _req;
	
	private HttpServletResponse _resp;
	
	private User _user;
	
	private List<String> _errors;
	
	private static final PersistenceManager _pm = PMF.get().getPersistenceManager();
	
 	public Datastore(HttpServletRequest req, HttpServletResponse resp, List<String> errors) {
 		
 		_req = req;
 		
 		_resp = resp;
 		
		_errors = errors;
		
		_user = findUser();
	}
 	
 	/**
 	 * @return Datastore._user
 	 */
 	public User getUser() {
 		
 		return _user;
 	}
	
	/**
	 * Returns a list of queried users.
	 * @param query
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<User> getUsers(String query) {
		
		Query q = _pm.newQuery(User.class);
		
		if(query != null) {
			
			q.setFilter(query);
		}
		
		return (List<User>) q.execute();
	}
	
	/**
	 * Returns a list of queried courses.
	 * @param query
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<Course> getCourses(String query) {

		Query q = _pm.newQuery(Course.class);
		
		if(query != null) {

			q.setFilter(query);
		}
		
		List<Course> courses = (List<Course>) q.execute();
		
		Collections.<Course>sort(courses);

		return courses;
 	}
	
	/**
	 * Returns a list of queried sections.
	 * @param query
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<Section> getSections(String query) {

		Query q = _pm.newQuery(Section.class);
		
		if(query != null) {
			
			q.setFilter(query);
		}
		
		return (List<Section>) q.execute();
 	}
	
	/**
	 * @return All users with access level 2, aka Instructors.
	 */
	public static List<User> getAllInstructors() {
		
 		return Datastore.getUsers("Access=='2'");
 	}

	/**
	 * @return A user searching by username based on logged-in cookie
	 */
	private User findUser() {
 		
 		String username = Form.getUserFromCookie(_req);
 		
 		String query = "UserName=='"+username+"'";
 		
 		return Datastore.getUsers(query).get(0);
	}
	
	/**
	 * Adds a course to datastore
	 * @param courseID
	 * @param name
	 */
	public void addCourse(String courseID, String name) {
		
		Course course = new Course(courseID, name);
		
		_pm.makePersistent(course);
	}

	/**
	 * Adds a section to datastore
	 * @param courseData This is an array of strings containing all details of a single course.
	 */
	public void addSection(String[] courseData) {
		
		String[] sectionNum = courseData[3].split(" ");
		String[] time = courseData[5].split("-");
		
		Section section = new Section();
		
		section.setID(courseData[0]);
		section.setName(courseData[1]);
		section.setUnits(courseData[2]);
		section.setClassNum(courseData[4]);
		section.setDay(courseData[6]);
		section.setInstructorID("");
		section.setLocation(courseData[9]);
		section.setCourseID(courseData[10]);
			
		section.setStartTime(time.length == 2 ? time[0] : "");
		section.setEndTime(time.length == 2 ? time[1] : "");
		
		section.setClassType(sectionNum.length == 2 ? sectionNum[0] : "");
		section.setSection(sectionNum.length == 2 ? sectionNum[1] : "");
		
		_pm.makePersistent(section);
	}
	
	/**
	 * Calls private method depending on servlet
	 * @param methodName
	 * @throws IOException
	 */
	public void callMethod(String methodName) throws IOException {
		
		if(_errors.size() == 0) {
			
			switch(methodName) {
			
				case "updateUser": this.updateUser(); break;
				case "addUser": this.addUser(); break;
				case "searchUser": this.searchUser(); break;
				case "editCourse": this.editCourse(); break;
				case "refreshCourses": this.refreshCourses(); break;
				case "updateProf": this.updateProf(); break;
				case "adminAssignTA": this.adminAssignTA(); break;
				case "profAssignTA": this.profAssignTA(); break;
				case "": break;
				default: throw new IOException("Datastore.callMethod: "+methodName+" not found");
			}
		}
	}
	
	public static void dummyData(){
		//List<Course> course = Datastore.getCourses("CourseID=='4'");
		//course.get(0).setUserID("7;10;15;9");
	}
	
	private void profAssignTA() {
		// TODO Auto-generated method stub

		String courseID = _req.getParameter("courseID");
		
		String professor = "";
		
		int i = 0;

		List<Section> sections = Datastore.getSections("CourseID=='"+courseID+"' && ClassType!='LEC'");

		for(Section section : sections){
			
			professor = _req.getParameter("prof"+i);
			
			section.setInstructorID(professor);
			
			_pm.makePersistent(section);
			
			i++;
		}
	}

	private void adminAssignTA() {
		// TODO Auto-generated method stub
		String courseID="";
		String[] ta=_req.getParameterValues("prof");
		if(ta!=null)
		{
		for(String name:ta){
			System.out.println(name);
			courseID+=name+";";
		}
		String course = _req.getParameter("courseID");
		List<Course> courses = Datastore.getCourses("CourseID=='"+course+"'");
		if(courses.get(0)!=null){
			courses.get(0).setUserID(courseID);
			_pm.makePersistent(courses.get(0));
		}	
		}
	}

	/**
	 * Scrape Courses from UWM
	 * @throws IOException
	 */
	private void refreshCourses() throws IOException {
		
		Scrape scrape = new Scrape(_req, _resp);
		
		scrape.getScheduleFromUWM();
	}

	/**
	 * Edits all properties of a single course.
	 * @throws IOException
	 */
	private void editCourse() throws IOException {
		
		String SectionID = _req.getParameter("SectionID");
		
		Section section = Datastore.getSections("SectionID=='"+SectionID+"'").get(0);
		
		LocalTime start = LocalTime.parse(_req.getParameter("start-start")+":"+_req.getParameter("start-end"),DateTimeFormat.forPattern("H:m"));
		
		LocalTime end = LocalTime.parse(_req.getParameter("end-start")+":"+_req.getParameter("end-end"),DateTimeFormat.forPattern("H:m"));
		
		if((start.getValue(0)+(start.getValue(1)/60)) - (end.getValue(0)+(end.getValue(1)/60)) < 0){
			throw new IOException("Datastore.editCourse: end time is before start time"+'\n'+" Start: "+start.toString("h:mm a")+"End:"+end.toString("h:mm a"));
		}
		
		//TODO set section fields
		section.setSection(_req.getParameter("Section"));
		section.setClassNum(_req.getParameter("ClassNum"));
		section.setUnits(_req.getParameter("Units"));
		section.setLocation(_req.getParameter("Location"));
		section.setStartTime(start.toString("h:mm a"));
		section.setEndTime(end.toString("h:mm a"));
		
		_pm.makePersistent(section);
		
		
	}

	/**
	 * Adds a user with level 3 access (Admin).
	 */
	public static void addAdmin() {
		
		if(Datastore.getUsers(null).size() == 0) {
			
			User user = new User();
			
			user.setID(newUserID());
			user.setUserName("admin.pass"); 
			user.setPassword( "pass"); 
			user.setFirstName( "admin");
			user.setMiddleName( "");
			user.setLastName( "pass");
			user.setEmail( "admin@uwm.edu");
			user.setLocation( "");
			user.setPhone( "");
			user.setAltPhone( "");
			user.setOfficeHour1( "Wed;0;00;0;00");
			user.setOfficeHour2( "Wed;0;00;0;00");
			user.setOfficeHour3( "Wed;0;00;0;00");
			user.setAccess("3");
			
			_pm.makePersistent(user);
		}
	}
	
	/**
	 * @param _userID
	 * @return A user searching by userID.
	 */
	public static User getUserFromID(String _userID){
			List<User> _user = Datastore.getUsers("UserID=='"+_userID+"'");
			if (_user.size()==0)
				return null;
			return _user.get(0);
	}
	
	/**
	 * Called when assign professor is saved
	 * Updates all section with professors
	 */
	private void updateProf(){

		String courseID = _req.getParameter("courseID");
		
		String professor = "";
		
		int i = 0;

		List<Section> sections = Datastore.getSections("CourseID=='"+courseID+"' && ClassType=='LEC'");

		for(Section section : sections){
			
			professor = _req.getParameter("prof"+i);
			
			section.setInstructorID(professor);
			
			_pm.makePersistent(section);
			
			i++;
		}
	}
	
	/**
	 * Edits all properties of a single user.
	 */
	private void updateUser() {
		
		User user = getUser();
		
		user.setID(newUserID());
//		user.setFirstName( _req.getParameter("FirstName"));
//		user.setLastName( _req.getParameter("LastName"));
//		user.setPassword( _req.getParameter("Password"));
		user.setMiddleName( _req.getParameter("MiddleName"));
		user.setEmail( _req.getParameter("Email"));
		user.setLocation( _req.getParameter("Location"));
		user.setPhone( _req.getParameter("Phone"));
		user.setAltPhone( _req.getParameter("AltPhone"));
		user.setOfficeHour1( Form.calcOfficeHours(1, _req));
		user.setOfficeHour2( Form.calcOfficeHours(2, _req));
		user.setOfficeHour3(Form.calcOfficeHours(3, _req));
		
		_pm.makePersistent(user);
	}

	/**
	 * Search user by name.
	 */
	private void searchUser() {
		
		String firstName = _req.getParameter("FirstName");
		
		String lastName = _req.getParameter("LastName");
		
		if(!userExists(firstName, lastName)) {

			_errors.add("No User Found");
		}
	}

	/**
	 * @param username
	 * @return true if user exists
	 */
	private boolean userExists(String firstname, String lastname) {
		
		List<User> users = Datastore.getUsers("FirstName=='"+ firstname +"' && LastName=='" + lastname + "'");

		return (users.size() != 0);
	}
 	
	/**
	 * Adds a generic user.
	 * If user already exists, add an error to {@link #_errors _errors}
	 */
	private void addUser() {
		
		String firstName = firstLetterUpperRestLowerFormat(_req.getParameter("FirstName"));
		String lastName = firstLetterUpperRestLowerFormat(_req.getParameter("LastName"));
		String access = _req.getParameter("Access");
		String email = _req.getParameter("Email");

		if(!userExists(firstName, lastName)) {
			
			User user = new User();
			
			user.setID(newUserID());
			user.setUserName(email.substring(0, email.indexOf('@')));
			user.setPassword( lastName); 
			user.setFirstName( firstName);
			user.setMiddleName( "");
			user.setLastName( lastName);
			user.setEmail(email);
			user.setLocation( "");
			user.setPhone( "");
			user.setAltPhone( "");
			user.setOfficeHour1( "Wed;0;00;0;00");
			user.setOfficeHour2( "Wed;0;00;0;00");
			user.setOfficeHour3( "Wed;0;00;0;00");
			user.setAccess(access);
			
			if(access.equalsIgnoreCase("1")){
				user.setKeyword(_req.getParameter("taSkills").toString());
			}
			_pm.makePersistent(user);
			
		} else {
			
			_errors.add("This person is already a user");
		}
	}
	
	/**
	 * Capitalize the first letter of a word
	 * @param - name
	 */
	public static String firstLetterUpperRestLowerFormat(String name){
		name = name.toLowerCase();
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	/**
	 * Deletes all courses
	 */
	public void deleteCourses() {
		
		List<Course> courses = Datastore.getCourses(null);
		List<Section> sections = Datastore.getSections(null);
		
		_pm.deletePersistentAll(courses);
		_pm.deletePersistentAll(sections);
	}
	
	/** 
	 * Generates a new userID.
	 * @return userID.
	 */
	private static String newUserID() {

		return (Datastore.getUsers(null).size() + 1) + "";
	}

	/**
	 * Assigns a photo to a user.
	 * @param blobKey
	 */
	public void setImage(String blobKey) {
		
		_user.setImage(blobKey);
	}

	public void addTestInstructor(String instructorName, String email, String classType) {
		
		String[] type = classType.split(" ");
		String[] name = instructorName.split(", ");
		
		if(name.length == 2 && email != null) {
			
			String firstName = name[1];
			String lastName = name[0];
			String username = email.substring(0, email.indexOf('@'));
			
			if(!userExists(firstName, lastName)) {
				
				User user = new User();
				
				user.setID(newUserID());
				System.out.println(user.getID());
				user.setUserName( username);
				user.setPassword( lastName); 
				user.setFirstName( firstName);
				user.setMiddleName( "");
				user.setLastName( lastName);
				user.setEmail(email);
				user.setLocation( "");
				user.setPhone( "");
				user.setAltPhone( "");
				user.setOfficeHour1( "Wed;0;00;0;00");
				user.setOfficeHour2( "Wed;0;00;0;00");
				user.setOfficeHour3( "Wed;0;00;0;00");
				user.setAccess("LEC".equals(type[0]) ? "2" : "1");
			
				_pm.makePersistent(user);
			}
		}
	}
}
