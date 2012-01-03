import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

public class IconMagic {

	public static void main(String... args) throws Exception {
		new IconMagic(args[0], args[1]).begin();
	}

	File platform;

	private Map<String, Object> platforms = new HashMap<String, Object>();

	public IconMagic(String platforms, String dbLoc) {
		this.platform = new File(platforms);
		this.dbLoc = dbLoc;
	}

	private String dbLoc;

	private Connection c;
	
	Map<String,Set<AndroidImage>> hashToImage = new HashMap<String,Set<AndroidImage>>();
	Map<String,Set<AndroidImage>> nameToImage = new HashMap<String,Set<AndroidImage>>();
	
	public void organizeImages() throws Exception {
		Statement s = c.createStatement();
		ResultSet rs = s.executeQuery("select platform,name,drawable_level,sha,width,height,length from images");
		
		while(rs.next()) {
			AndroidImage i = new AndroidImage();
			i.platform = rs.getString(1);
			i.name = rs.getString(2);
			i.drawableLevel = rs.getString(3);
			i.sha = rs.getString(4);
			i.width = rs.getInt(5);
			i.height = rs.getInt(6);
			i.length = rs.getInt(7);
			Set<AndroidImage> images = hashToImage.get(i.sha);
			if(images==null) {
				images = new LinkedHashSet<AndroidImage>();
				hashToImage.put(i.sha, images);
			}
			images.add(i);
			images = nameToImage.get(i.name);
			if(images==null) {
				images = new LinkedHashSet<AndroidImage>();
				nameToImage.put(i.name,images);
			}
			images.add(i);
			
		}
		int dupes=0;
		for(Map.Entry<String, Set<AndroidImage>> e : hashToImage.entrySet()) {
			if(e.getValue().size()>1) {
				System.out.println(e.getValue().size());
				dupes++;
			}
		}
		System.out.println(dupes);
		System.out.println(hashToImage.size());
		System.out.println(nameToImage.size());
	}

	public void begin() throws Exception {
		Class.forName("org.sqlite.JDBC");
		c = DriverManager.getConnection("jdbc:sqlite:" + dbLoc);
		Statement s = c.createStatement();
		s.execute("create table if not exists images(width integer, height integer, length integer, sha varchar(40), platform varchar(20), name varchar(50), drawable_level varchar(30))");
		s.execute("create table if not exists completed(completed integer)");
		ResultSet rs = s.executeQuery("select count(*) from completed");		
		int count = rs.getInt(1);
		boolean hasEntries = count>0;
		rs.close();
		if(!hasEntries) {
			s.execute("delete from images");
			System.out.println(new Date());
			File[] files = platform.listFiles(new FileFilter() {

				@Override
				public boolean accept(File pathname) {
					return pathname.isDirectory();
				}

			});
			for (File file : files) {
				handlePlatform(file);
			}
			System.out.println(new Date());
			s.execute("insert into completed(completed) values(1)");
		} else {
			
		}
		organizeImages();
		
		
	}

	private void handlePlatform(File file) {
		File resources = new File(file, "data/res");
		File[] files = resources.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return pathname.getName().startsWith("drawable");
			}

		});
		for (File f : files) {
			handleDrawables(file.getName(), f.getName(), f);
		}
	}

	private void handleDrawables(String platform, String drawableLevel, File f) {
		File[] files = f.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				String name = pathname.getName();
				return name.endsWith(".png");
			}
		});
		ImageInfo inf = new ImageInfo();
		try {			
			PreparedStatement pr = c.prepareStatement(
					"insert into images(width,height,length,sha,platform," +
					"name,drawable_level) values(?,?,?,?,?,?,?)");
			for (File file : files) {				
				try {
					FileInputStream fis = new FileInputStream(file);
					inf.setInput(fis);
					inf.check();
					// System.out.println(inf.getWidth()+"x"+inf.getHeight());
					fis.close();
					Process p = Runtime.getRuntime().exec(
							new String[] { "/usr/bin/shasum",
									file.getAbsolutePath() });
					p.waitFor();
					BufferedReader br = new BufferedReader(
							new InputStreamReader(p.getInputStream()));
					AndroidImage i = new AndroidImage();
					i.width = inf.getWidth();
					i.height = inf.getHeight();
					i.platform = platform;
					i.length = file.length();
					i.name = getFilePrefix(file.getName());
					i.drawableLevel = drawableLevel;
					i.sha = br.readLine().split(" ")[0];
					pr.setInt(1, i.width);
					pr.setInt(2,i.height);
					pr.setLong(3,i.length);
					pr.setString(4, i.sha);
					pr.setString(5, i.platform);
					pr.setString(6, i.name);
					pr.setString(7, i.drawableLevel);
					pr.execute();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Dimension getImageDim(final String path) {
		Dimension result = null;
		String suffix = this.getFileSuffix(path);
		Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix);
		if (iter.hasNext()) {
			ImageReader reader = iter.next();
			try {
				ImageInputStream stream = new FileImageInputStream(new File(
						path));
				reader.setInput(stream);
				int width = reader.getWidth(reader.getMinIndex());
				int height = reader.getHeight(reader.getMinIndex());
				result = new Dimension(width, height);
			} catch (IOException e) {
				// log(e.getMessage());
			} finally {
				reader.dispose();
			}
		} else {
			// log("No reader found for given format: " + suffix));
		}
		return result;
	}

	private String getFileSuffix(final String path) {
		String result = null;
		if (path != null) {
			result = "";
			if (path.lastIndexOf('.') != -1) {
				result = path.substring(path.lastIndexOf('.'));
				if (result.startsWith(".")) {
					result = result.substring(1);
				}
			}
		}
		return result;
	}

	private String getFilePrefix(final String path) {
		String result = null;
		if (path != null) {
			result = "";
			if (path.lastIndexOf('.') != -1) {
				result = path.substring(0, path.lastIndexOf('.'));
			}
		}
		return result;
	}

}
