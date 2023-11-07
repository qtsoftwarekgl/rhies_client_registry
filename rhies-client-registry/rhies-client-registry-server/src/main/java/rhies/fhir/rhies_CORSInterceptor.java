package rhies.fhir;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(asyncSupported = true, urlPatterns = { "/*" })
public class rhies_CORSInterceptor implements Filter {
   private static String[] allowedOrigins = Constants.ALLOWED_ORIGINS;
   public rhies_CORSInterceptor() {
      allowedOrigins = System.getenv("allowedOrigins").split(",");
   }


   @Override
   public void init(FilterConfig filterConfig) throws ServletException {

   }

   @Override
   public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
      HttpServletRequest request = (HttpServletRequest) servletRequest;

      String requestOrigin = request.getHeader("Origin") == null?"":request.getHeader("Origin");
      ;

      //System.out.println(request.getHeader().toString());
      if( isAllowedOrigin(requestOrigin)){
         ((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Origin", requestOrigin);
         ((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Headers", "*");
         ((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");

         HttpServletResponse response = (HttpServletResponse) servletResponse;

         if (request.getMethod().equals("OPTIONS")) {
            response.setStatus(HttpServletResponse.SC_ACCEPTED);
            return;
         }
      }
      filterChain.doFilter(request, servletResponse);
   }

   @Override
   public void destroy() {

   }

   private boolean isAllowedOrigin(String origin){
      for (String allowedOrigin : allowedOrigins) {
         if(origin.equals(allowedOrigin)) return true;
      }
      return false;
   }
}
