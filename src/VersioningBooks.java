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
import java.text.MessageFormat;

public class VersioningBooks{
  String rootPath;
  String FSRepoURI;
  public void addAll(String rootPath,String FSRepoURI) {
    this.rootPath=rootPath;
    this.FSRepoURI=FSRepoURI;

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
      commitEditor = repos.getCommitEditor("initilizing or refreshing the repository with Ciel",null,false,null,null);
      commitEditor.openRoot(SVNRepository.INVALID_REVISION);
    }catch(SVNException e){
      System.err.println("error occured while creating ISVNEditor class object");
    }
    SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();

    File rootPathF = new File(rootPath);
    File[] list = rootPathF.listFiles();
    if(list==null ) System.err.println("there is no file");
    Queue<File> que = new LinkedList<File>();
    que.add(rootPathF);
    SVNNodeKind nk=null;
    SVNRepository _repos=null;
    try{
      _repos = SVNRepositoryFactory.create(reposURL);
    }catch(SVNException e){
      System.err.println("error occured while creating _repos");
      e.printStackTrace();
    }
    while(que.size()>0){
      File f = que.poll();
      if(f.isDirectory()){
	try{
	  String repopath = f.getAbsolutePath().replace(rootPathF.getAbsolutePath(),"");
	  if(f.getName().startsWith(".")) continue;
	  nk = _repos.checkPath(repopath,SVNRepository.INVALID_REVISION);
	  if(nk.compareTo(SVNNodeKind.DIR)==0){
	    // if DIR exists
	    System.err.println("directory already exists.");
	    
	    // delete deleted files from repository
	    String[] filelist = f.list();
	    if(repopath.length()>1 && repopath.charAt(repopath.length()-1)!='/') repopath+="/";
	    Collection<SVNDirEntry> rlist=_repos.getDir(repopath,SVNRepository.INVALID_REVISION,null,(Collection)null);
	    Iterator<SVNDirEntry> ite = rlist.iterator();
	    SVNDirEntry repo_file=null;
	    while(ite.hasNext()){
	      repo_file=ite.next();
	      if( ! new File(rootPath,repopath+repo_file.getName() ).exists() ){
		try{
		  commitEditor.deleteEntry(repopath+repo_file.getName(),SVNRepository.INVALID_REVISION );
		}catch(SVNException e){
		  System.out.println("failed to delete");
		  e.printStackTrace();
		}
	      }
	    }
	  }else{
	    // if DIR does not exist
	    commitEditor.addDir(repopath,null,SVNRepository.INVALID_REVISION);
	    commitEditor.closeDir();
	  }
	  list = f.listFiles();
	  for(int i=0;i<list.length;i++)  que.offer(list[i]);
	}catch(SVNException e){
	  System.err.println("error occured while creating directory");
	  e.printStackTrace();
	}
      }else{
	try{
	  String repopath = f.getAbsolutePath().replace(rootPathF.getAbsolutePath(),"");
	System.out.println("add file : "+repopath);
	  File file = new File(rootPath,repopath);
	  String checksum=null;
	  nk = _repos.checkPath(repopath,SVNRepository.INVALID_REVISION);
	  if(nk.compareTo(SVNNodeKind.FILE)==0){
	    commitEditor.openFile(repopath,SVNRepository.INVALID_REVISION);
	    commitEditor.applyTextDelta(repopath,null);
	    checksum = deltaGenerator.sendDelta(repopath,new FileInputStream(file),commitEditor,true);
	    //	    checksum = deltaGenerator.sendDelta(repopath,null,0,
	    //			     new FileInputStream(file),commitEditor,true);
	  }else{
	    commitEditor.addFile(repopath,null,SVNRepository.INVALID_REVISION);
	    commitEditor.applyTextDelta(repopath,null);
	    checksum = deltaGenerator.sendDelta(repopath,new FileInputStream(file),commitEditor,true);
	  }
	  System.out.println(repopath+" checksum:"+checksum);
	  commitEditor.closeFile(repopath,checksum);
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
    if(args.length<2){
      StringBuffer msg = new StringBuffer();
      msg.append("usage:\n");
      msg.append("java -jar VersioningBooks <DirPath> <RepoPath>\n");
      System.err.println(msg.toString());
      System.exit(0);
    }
    VersioningBooks vb = new VersioningBooks();
    //    vb.addAll("/home/yama/diva/workspace/Ciel/WEB-INF/CielFiles","/home/yama/diva/work/SVNKit/repos");
    System.out.println(MessageFormat.format("params: {0} {1}",(Object[])args));
    vb.addAll(args[0],args[1]);
  }
}