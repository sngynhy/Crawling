package crawling;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import model.common.JDBC;

public class Crawling {
	
	public static void main(String[] args) {

		Connection conn = JDBC.getConnection();
		PreparedStatement pstmt = null;

		String url = "https://movie.naver.com/movie/running/current.naver"; // 영화 리스트
		String url2 = "https://movie.naver.com";

		Document doc = null;
		Document doc2 = null;

		String [][] mpkSet= {		//유지보수를 위해  mpkSet 선언
				{"액션","애니메이션","멜로/로멘스","드라마","다큐멘터리"},
				{"AC","AN","RO","DR","DC"}
		};
		Boolean isMpk = false;		//기타 장르 선별
		
		String mpkStr = null;	//	mpk
		String mpkType = null;	//  'AC' 등 1001 앞에 들어갈 장르키
		int mpkInt = 1001;
		
		try {
			doc = Jsoup.connect(url).get();
			Elements imgUrl = doc.select(".thumb"); // 각 이미지 url 목록
			//System.out.println("imgUrl" + imgUrl);
			//System.out.println(imgUrl.size());
			for (int i=0; i<imgUrl.size(); i++) {
				doc2 = Jsoup.connect(url2 + imgUrl.get(i).select("a").attr("href")).get(); // 각 이미지 클릭 시 이동되는 해당 영화 정보 페이지 document

				Element el = doc2.select(".mv_info_area").get(0);
				String title = el.select(".h_movie > a").first().text(); // 타이틀
				System.out.println("타이틀: " + title);

				Elements info = el.select(".info_spec > dd").first().select("span"); // 영화 정보 - 장르, 제작국, 러닝타임, 개봉날짜
				//				System.out.println("info : " + info);

				String genre = null; // 장르
				String country = null; // 제작국
				String runtime = null; // 러닝타임
				String date = null; // 개봉 날짜

				if (info.size() != 4) {
					continue;
				}
				for (int j=0; j<info.size(); j++) { // 장르, 제작국, 러닝타임, 개봉날짜
					//					String a = info.get(j).text();
					//					System.out.println(j + " : " + a);
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

//				String summary = doc2.select(".con_tx").size() > 0 ? doc2.select(".con_tx").first().text() : null; // 줄거리 없는 경우 null로 처리
				String summary = "";
				if (doc2.select(".con_tx").size() > 0) {
					summary = doc2.select(".con_tx").first().text();
				} else {
					continue;
				}
				
				System.out.println("줄거리: " + summary);
				
				String poster = el.select(".poster img").attr("src"); // 영화 포스터 URL
				poster = poster.substring(0,poster.lastIndexOf("?"));
				System.out.println("포스터: " + poster);				
				
				for(int k = 0 ; k < mpkSet[0].length; k++) {
					if(genre.equals(mpkSet[0][k])) {
						mpkType = mpkSet[1][k];
						isMpk = true;
					}
				}
				
				if(!isMpk) {
					mpkType = "ECT";
				}
				
				mpkStr = mpkType + mpkInt;
				mpkInt++;

				System.out.println("mpk : " + mpkStr);
				
				System.out.println();
				
				// insert into movie2 values (mpk, 'title', 'summary', 'genre', to_date('2021/09/29', 'YYYY/MM/DD'), '포스터URL');
				pstmt = conn.prepareStatement("INSERT INTO MOVIE VALUES (?,?,?,?,to_date(?, 'YYYY/MM/DD'),?)");
				pstmt.setString(1, mpkStr);
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
