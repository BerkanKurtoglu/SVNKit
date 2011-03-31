/**
 * @ref http://www.jarvana.com/jarvana/view/org/codehaus/jtstand/jtstand-svnkit/1.1/jtstand-svnkit-1.1-javadoc.jar!/index.html?org/tmatesoft/svn/core/internal/io/fs/FSRepositoryFactory.html
 **/
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.io.*;
import org.tmatesoft.svn.core.wc.*;
import org.tmatesoft.svn.core.auth.*;
import org.tmatesoft.svn.core.internal.io.fs.*;
import org.tmatesoft.svn.core.io.diff.*;
import org.tmatesoft.svn.core.internal.wc.admin.SVNWCAccess;
import org.tmatesoft.svn.core.internal.wc.*;
import org.tmatesoft.svn.core.wc.admin.*;

import java.io.*;
import java.util.*;

/*
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.SVNException;
*/
public class SVNKit{
  final static String SVNRepoURI;
  final static String FSRepoURI;
  static {
    SVNRepoURI="svn://host/repo";
    FSRepoURI="/home/yama/diva/work/SVNKit/repos";
  }
  /*
  public void svn(){
    try{
      SVNRepositoryFactoryImpl.setup();
      SVNURL url = SVNURL.parseURIDecoded();
    }catch(Exception e){
      e.printStackTrace();
    }
  }
  */
  public class NullWorkspace implements ISVNWorkspaceMediator{
    public SVNPropertyValue getWorkspaceProperty(String path,
						 String name) throws SVNException{
      return SVNPropertyValue.create("");
    }
    public void setWorkspaceProperty(String path,
                                 String name,
                                 SVNPropertyValue value) throws SVNException{}
  }
  /*
   * @ref http://d.hatena.ne.jp/reiro/20080218/1203321200
   */
  public void sample(){
    try{
      FSRepositoryFactory.setup();
      SVNRepository repository = FSRepositoryFactory.create(SVNURL.parseURIEncoded(FSRepoURI));
      try{
	repository.testConnection();
      }catch(SVNException e){
	System.err.println("failed to connect the repository.");
      }
      ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager("", "");
      repository.setAuthenticationManager(authManager);
      ISVNEditor editor = repository.getCommitEditor("test", new NullWorkspace()); 
      editor.openRoot(-1);
      editor.addDir("10",null,-1);
      editor.addFile("10/Book1.xls",null,-1);
      editor.applyTextDelta("10/Book1.xls",null);
      SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
      //String checksum = deltaGenerator.sendDelta("10/Book1.xls",new ByteArrayInputStream("sampleBook".getBytes()),editor,true);
      String checksum = deltaGenerator.sendDelta("10/Book1.xls",new FileInputStream("./Book1.xls"),editor,true);
      editor.closeFile("10/Book1.xls",checksum);
      editor.closeDir();
      editor.closeDir();
      editor.closeEdit();

    }catch(Exception e){
      e.printStackTrace();
    }
  }
  /**
   *
   * @param rootpath the path of directory in working directory
   * @param filepath the path of file from working directory
   **/
  public void fs(String rootpath,String filepath){
    System.out.println("SVNKit#fs starts");
    try{
      FSRepositoryFactory.setup();

      File reposRoot = new File(FSRepoURI);
      // svnadmin create
      SVNAdminClient adminClient = SVNClientManager.newInstance().getAdminClient();
      adminClient.doCreateRepository(reposRoot,null,true,true,false,false);
      SVNURL reposURL = SVNURL.fromFile(reposRoot);
      
      // svn add 
      SVNRepository repos = SVNRepositoryFactory.create(reposURL);
      ISVNEditor commitEditor = repos.getCommitEditor("initializing the repository with a greek tree",null,false,null,null);
      commitEditor.openRoot(SVNRepository.INVALID_REVISION);

      if(filepath.indexOf("/")!=-1){
	String dir = filepath.substring(0,filepath.lastIndexOf("/"));
	System.out.println("dir="+dir);
	String[] d=dir.split("/");
	String tmp="";
	for(int i=0;i<d.length;i++){
	  tmp+=d[i]+"/";
	  commitEditor.addDir(tmp,null,SVNRepository.INVALID_REVISION);
	}
      }
      //String fileName = "Book1.xls";
      commitEditor.addFile(filepath,null,SVNRepository.INVALID_REVISION);
      SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
      File file = new File(rootpath,filepath);
      commitEditor.applyTextDelta(filepath,null);
      String checksum = deltaGenerator.sendDelta(filepath,new FileInputStream(file),commitEditor,true);
      commitEditor.closeFile(filepath,checksum);

      commitEditor.closeDir();
      commitEditor.closeEdit();
      System.out.println("SVNKit#fs finished");
    }catch(Exception e){
      e.printStackTrace();
    }
  }

  public void addAll(String rootPath) {
    FSRepositoryFactory.setup();
    File reposRoot = new File(FSRepoURI);
    SVNAdminClient adminClient = SVNClientManager.newInstance().getAdminClient();
    try{
      adminClient.doCreateRepository(reposRoot,null,true,false,false,false);
    }catch(SVNException e){
      System.err.println("repository already exists");
    }
    SVNURL reposURL=null;
    SVNRepository repos=null;
    try{
      reposURL = SVNURL.fromFile(reposRoot);
      // svn add 
      repos = SVNRepositoryFactory.create(reposURL);
    }catch(SVNException e){
      System.err.println("error occured while connecting repository");
      e.printStackTrace();
    }
    ISVNEditor commitEditor=null;
    try{
      commitEditor = repos.getCommitEditor("initializing the repository with Ciel",null,false,null,null);
      commitEditor.openRoot(SVNRepository.INVALID_REVISION);
    }catch(SVNException e){
      System.err.println("error occured while creating ISVNEditor class object");
    }
    
    SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();

    File rootPathF = new File(rootPath);
    File[] list = rootPathF.listFiles();
    if(list==null ) System.err.println("there is no file");
    Queue<File> que = new LinkedList<File>();
    for(int i=0;i<list.length;i++) que.offer(list[i]);
    while(que.size()>0){
      File f = que.poll();
      if(f.isDirectory()){
	try{
	String filepath = f.getAbsolutePath().replace(rootPathF.getAbsolutePath(),"");
	System.out.println("add dir : filepath : "+filepath);
	if(f.getName().startsWith(".")) continue;
	commitEditor.addDir(filepath,null,SVNRepository.INVALID_REVISION);
	commitEditor.closeDir();
	list = f.listFiles();
	for(int i=0;i<list.length;i++)  que.offer(list[i]);
	}catch(SVNException e){
	  System.err.println("error occured while creating directory");
	  e.printStackTrace();
	}
      }else{
	try{
	  String filepath = f.getAbsolutePath().replace(rootPathF.getAbsolutePath(),"");
	  File file = new File(rootPath,filepath);
	  commitEditor.addFile(filepath,null,SVNRepository.INVALID_REVISION);
	  commitEditor.applyTextDelta(filepath,null);
	  String checksum = deltaGenerator.sendDelta(filepath,new FileInputStream(file),commitEditor,true);
	  commitEditor.closeFile(filepath,checksum);	  
	}catch(SVNException e){
	  System.err.println("error occured while adding file");
	  e.printStackTrace();
	}catch(FileNotFoundException e){
	  System.err.println("error occured while adding file");
	  e.printStackTrace();
	}
	
      }
    }
    try{
      commitEditor.closeEdit();
    }catch(Exception e){}
    
  }
  public static void main(String[] args) throws Exception{
    //new SVNKit().svn();
    //new SVNKit().fs("/home/yama/diva/work/SVNKit","10/2011503/Book1.xls");

    new SVNKit().addAll("/home/yama/diva/workspace/Ciel/WEB-INF/CielFiles");
  }
}
