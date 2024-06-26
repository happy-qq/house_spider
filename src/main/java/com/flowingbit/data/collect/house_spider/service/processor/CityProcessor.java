package com.flowingbit.data.collect.house_spider.service.processor;

import com.flowingbit.data.collect.house_spider.dao.RedisDAO;
import com.flowingbit.data.collect.house_spider.model.City;
import com.flowingbit.data.collect.house_spider.utils.IOUtil;
import net.minidev.json.JSONArray;
import org.apache.commons.lang3.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Selectable;

import java.util.ArrayList;
import java.util.List;

public class CityProcessor implements PageProcessor {

    private List<City> cityList;

    RedisDAO redisDAO = new RedisDAO();

    public CityProcessor(){
        this.cityList = new ArrayList<>();
    }

    // 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等
    private Site site = Site.me()
            .setRetryTimes(3)
            .setSleepTime(1000)
            .addHeader("Accept-Encoding", "gzip, deflate, br")
            .addHeader("Accept-Language", "zh-CN,zh;q=0.9")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
            .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.103 Safari/537.36");

    /**
     * process是定制爬虫逻辑的核心接口，在这里编写抽取逻辑
     */
    @Override
    public void process(Page page) {
        try {
            // 部分二：定义如何抽取页面信息，并保存下来
            if (!page.getHtml().xpath("//div[@class='city_list']").match()){
                page.setSkip(true);
            }
            //将html输出到文件
            // C:/Users/flowi/Desktop/lianjia.html
            //IOUtil.outFile(page.getHtml().toString(), "/Users/zhaoluyang/Desktop/lianjia-header.html");
            //开始提取页面信息
            System.out.println(page.getUrl().toString());
            List<Selectable> provinces = page.getHtml().xpath("//div[@class='city_province']").nodes();
            provinces.forEach(e -> {
                String province = e.xpath("//div[@class='city_list_tit c_b']/text()").toString();
                System.out.println("省份："+province);
                List<Selectable> citys = e.xpath("//ul[1]/li").nodes();
                citys.forEach(f->{
                    City city = new City();
                    String cityName = f.xpath("//a/text()").toString();
                    String cityUrl = f.xpath("//a/@href").toString();
                    city.setName(cityName);
                    city.setBriefName(StringUtils.substringBetween(cityUrl, "https://","."));
                    System.out.println("  |——城市："+cityName + " " + cityUrl);
                    cityList.add(city);
                });

            });
            //存到redis
            redisDAO.setList("citys", cityList);
            //存成json文件
            String jsonstr = JSONArray.toJSONString(cityList);
            IOUtil.outFile(jsonstr, "citys.json");
        }catch (Exception ee){
            throw new RuntimeException("CityProcessor Error >> ",ee);
            //EmailService.sendMail("769010256@qq.com", page.getUrl().toString(), eee.toString());
        }
    }


    @Override
    public Site getSite() {
        return site;
    }

    public void startProcessor(String url){
        Spider.create(new CityProcessor())
                //从"https://github.com/code4craft"开始抓
                .addUrl(url)
                //开启2个线程抓取
                .thread(2)
                //启动爬虫
                .run();
    }

    public static void main(String[] args){
        Spider.create(new CityProcessor())
                //从"https://www.lianjia.com/city/"开始抓
                .addUrl("https://www.lianjia.com/city/")
                //开启2个线程抓取
                .thread(1)
                //启动爬虫
                .run();
    }

}
