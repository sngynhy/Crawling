package crawling;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import model.common.JDBC;

public class Movie {
	
	public static void main(String[] args) {

		Connection conn = JDBC.getConnection();
		PreparedStatement pstmt = null;

		String url = "https://movie.naver.com/movie/running/current.naver"; // 영화 리스트
		String url2 = "https://movie.naver.com";
		
		Document doc = null;

		HashMap<String, String> map = new HashMap<String, String>();
		map.put("액션", "AC");
		map.put("애니메이션", "AN");
		map.put("멜로/로맨스", "RO");
		map.put("드라마", "DR");
		map.put("다큐멘터리", "DC");
		
		int mpkInt = 1001;
		
		try {
			doc = Jsoup.connect(url).get();
			Elements imgUrl = doc.select(".thumb"); // 각 이미지 url 목록
//			System.out.println("imgUrl" + imgUrl);
			//System.out.println(imgUrl.size());
			for (int i=0; i<imgUrl.size(); i++) {
				doc = Jsoup.connect(url2 + imgUrl.get(i).select("a").attr("href")).get(); // 각 이미지 클릭 시 이동되는 해당 영화 정보 페이지 document				
				
				// 영화 세부 정보 크롤링
				Element el = doc.select(".mv_info_area").get(0);
				String title = el.select(".h_movie > a").first().text(); // 타이틀
				System.out.println("타이틀: " + title);

				Elements info = el.select(".info_spec > dd").first().select("span"); // 영화 정보 - 장르, 제작국, 러닝타임, 개봉날짜
				//System.out.println("info : " + info);

				String genre = null; // 장르
				String country = null; // 제작국
				String runtime = null; // 러닝타임
				String date = null; // 개봉 날짜

				if (info.size() != 4) {
					continue;
				}
				for (int j=0; j<info.size(); j++) { // 장르, 제작국, 러닝타임, 개봉날짜
					//String a = info.get(j).text();
					//System.out.println(j + " : " + a);
					genre = info.get(0).text();
					country = info.get(1).text();
					runtime = info.get(2).text();
					date = info.get(3).text();
				}

				if (genre.indexOf(",") > 0) { // 장르 여러개일 경우 맨 첫번째 값만 저장
					genre = genre.substring(0, genre.indexOf(","));
				}

				//System.out.println(date.indexOf("개"));
				date = date.substring(0,11);
				date = date.replace(".", "/");
				date = date.replace(" ", "");

				System.out.println("장르: " + genre);
				System.out.println("제작국: " + country);
				System.out.println("러닝타임: " + runtime);
				System.out.println("개봉날짜: " + date);

				String summary = "";
				if (doc.select(".con_tx").size() > 0) {
					summary = doc.select(".con_tx").first().text();
				} else {
					continue;
				}
				
				System.out.println("줄거리: " + summary);
				
				String poster = el.select(".poster img").attr("src"); // 영화 포스터 URL
				poster = poster.substring(0,poster.lastIndexOf("?"));
				System.out.println("포스터: " + poster);							
				
				String mpk = "";
				if (map.containsKey(genre)) {
					mpk = map.get(genre) + mpkInt;
				} else {
					mpk = "EX" + mpkInt;
					genre = "ECT";
				}
				mpkInt++;
				System.out.println(mpk);
				
				System.out.println();
				
				// insert into movie2 values (mpk, 'title', 'summary', 'genre', to_date('2021/09/29', 'YYYY/MM/DD'), '포스터URL');
				String sql = "INSERT INTO MOVIE VALUES (?,?,?,?,to_date(?, 'YYYY/MM/DD'),?)";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, mpk);
				pstmt.setString(2, title);
				pstmt.setString(3, summary);
				pstmt.setString(4, genre);
				pstmt.setString(5, date);
				pstmt.setString(6, poster);
				pstmt.executeUpdate();
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			JDBC.close(conn, pstmt);
		}

	}
}
