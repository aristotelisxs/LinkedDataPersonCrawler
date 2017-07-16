list.of.packages <- c("ggplot2", "reshape2", "lubridate", "tsoutliers", "fma", "grid", "ellipse", "xtable", "Hmisc")
new.packages <- list.of.packages[!(list.of.packages %in% installed.packages()[,"Package"])]
if(length(new.packages)) install.packages(new.packages)

library(ggplot2)
library(lubridate)
library(tsoutliers)
library(fma)
library(reshape2)

# Multiple plot function
#
# ggplot objects can be passed in ..., or to plotlist (as a list of ggplot objects)
# - cols:   Number of columns in layout
# - layout: A matrix specifying the layout. If present, 'cols' is ignored.
#
# If the layout is something like matrix(c(1,2,3,3), nrow=2, byrow=TRUE),
# then plot 1 will go in the upper left, 2 will go in the upper right, and
# 3 will go all the way across the bottom.
#
multiplot <- function(..., plotlist=NULL, file, cols=1, layout=NULL) {
  library(grid)
  
  # Make a list from the ... arguments and plotlist
  plots <- c(list(...), plotlist)
  
  numPlots = length(plots)
  
  # If layout is NULL, then use 'cols' to determine layout
  if (is.null(layout)) {
    # Make the panel
    # ncol: Number of columns of plots
    # nrow: Number of rows needed, calculated from # of cols
    layout <- matrix(seq(1, cols * ceiling(numPlots/cols)),
                     ncol = cols, nrow = ceiling(numPlots/cols))
  }
  
  if (numPlots==1) {
    print(plots[[1]])
    
  } else {
    # Set up the page
    grid.newpage()
    pushViewport(viewport(layout = grid.layout(nrow(layout), ncol(layout))))
    
    # Make each plot, in the correct location
    for (i in 1:numPlots) {
      # Get the i,j matrix positions of the regions that contain this subplot
      matchidx <- as.data.frame(which(layout == i, arr.ind = TRUE))
      
      print(plots[[i]], vp = viewport(layout.pos.row = matchidx$row,
                                      layout.pos.col = matchidx$col))
    }
  }
}

wd <- "C:/Users/Ares Xs/workspace_Masters/LinkedDataPersonCrawler/R Analysis"
setwd(wd)

analytics_file <- "analytics.csv"
analytics <- read.table(analytics_file, header = T, row.names = NULL, sep = ",")
analytics$From_Country <- factor(analytics$From_Country)
analytics$Branches_Factor <- 0

# level of depth 1 - 5 with 1 being shallow and 5 being deep
for(i in 1:nrow(analytics)){
  if(analytics$Branching_Times[i]<=3){
    analytics$Branches_Factor[i] <- 1  
  } else if(analytics$Branching_Times[i]>3 && analytics$Branching_Times[i]<=6) {
    analytics$Branches_Factor[i] <- 2
  } else if(analytics$Branching_Times[i]>6 && analytics$Branching_Times[i]<=9) {
    analytics$Branches_Factor[i] <- 3
  } else if(analytics$Branching_Times[i]>9 && analytics$Branching_Times[i]<=12) {
    analytics$Branches_Factor[i] <- 4
  } else {
    analytics$Branches_Factor[i] <- 5
  }    
}

analytics$Branches_Factor <- factor(analytics$Branches_Factor)
analytics$Date <- dmy_hms(analytics$Date)

library(RColorBrewer)
myColors <- brewer.pal(5, "Paired")
names(myColors) <- levels(analytics$Branches_Factor)
colScaleBranches <- scale_colour_manual(name = "Branches_Factor",values = myColors)

analytics_moreThan100 <- analytics[analytics$People_Found > 25, ]

# Crawling duration grouped by filtered and un-filtered queries
crawl_per_query_type <- ggplot(analytics_moreThan100) + geom_histogram(aes(x = Crawling_Duration, 
                              fill = From_Country), bin = 5) +
  labs(title = "Crawling duration grouped by filtered and un-filtered queries",
       x = "Crawling Duration (seconds)", y = "Number of occurrences") +
  theme_bw(base_size = 10)

ggsave("Crawling duration grouped by query type.png", crawl_per_query_type)

# People found frequency categorised by branching type (from shallow to deep level)
peopleFound_branching <- ggplot(analytics) + geom_histogram(aes(x = People_Found, 
                                       fill = Branches_Factor), bin = 5) +
  labs(title = "People found by branching category (low, medium, high)",
       x = "Number of branches", y = "Number of occurrences") +
  theme_bw(base_size = 10)

ggsave("People found by branching type.png", peopleFound_branching)

# Crawling duration per People found categorised by branching type
crawl_duration_branching <- ggplot(analytics, aes(x = Crawling_Duration, y = People_Found,
                      colour = Branches_Factor, 
                      fill = Branches_Factor), bin = 1) + geom_point(size = 5) +
  labs(title = "Crawling duration per People found",
       x = "Crawling duration (seconds)", y = "People Found") +
  theme_bw(base_size = 10) + colScaleBranches

ggsave("Crawling duration by People Found (brancing type categorised).png", crawl_duration_branching)

# Crawling duration per People found categorised by query type
crawl_duration_query <- ggplot(analytics, aes(x = Crawling_Duration, y = People_Found,
                                                  colour = From_Country, 
                                                  fill = From_Country), bin = 1) + geom_point(size = 5) +
  labs(title = "Crawling duration per People found",
       x = "Crawling duration (seconds)", y = "People Found") +
  theme_bw(base_size = 10)

ggsave("Crawling duration by People Found (query type categorised).png", crawl_duration_query)

# People required per People found categorised by branching type
people_required_branching <- ggplot(analytics, aes(x = People_Required, y = People_Found,
                      colour = Branches_Factor, 
                      fill = Branches_Factor), bin = 1) + geom_point(size = 5) +
  labs(title = "People required per People found",
       x = "People Required", y = "People Found") +
  theme_bw(base_size = 10) + colScaleBranches

ggsave("People required per People found categorised by branching type.png", people_required_branching)

# Added_URIs per People found categorised by branching type
addedURI_peopleFound <- ggplot(analytics, aes(x = Added_URIs, y = People_Required,
                      colour = Branches_Factor, 
                      fill = Branches_Factor), bin = 1) + geom_point(size = 5) +
  labs(title = "Added_URIs per People found",
       x = "Added_URIs", y = "People Found") +
  theme_bw(base_size = 10) + colScaleBranches

ggsave("Added_URIs per People found categorised by branching type.png", addedURI_peopleFound)

branching_perPeople <- ggplot(analytics) + 
  geom_point(aes(x = Branching_Times, y = People_Found, colour = Branches_Factor), size = 4) +
  labs(title = "People Found per Branching Type",
       x = "Branching Times", y = "People Found") +
  theme_bw(base_size = 10)

ggsave("Branching times per People Found.png", branching_perPeople)

multiplot(crawl_duration_query, crawl_duration_branching, col = 1)

long_crawlPerKeyword <- melt(analytics, id=c("Keyword", "Crawling_Duration", "People_Found", "Branches_Factor"))

boxPlot_Crawling_Keyword <- ggplot(long_crawlPerKeyword, aes(x=Keyword, y = Crawling_Duration, fill = Branches_Factor)) + 
  geom_boxplot() + theme(axis.text.x = element_text(angle = 90, hjust = 1)) + 
  labs(title = "Distribution of Crawling time per Keyword",
                                  y = "Crawling Duration") 

ggsave("Distribution of Crawling time per Keyword.png", boxPlot_Crawling_Keyword)

boxPlot_People_Keyword <- ggplot(long_crawlPerKeyword, aes(x=Keyword, y = People_Found, fill = Branches_Factor)) + 
  geom_boxplot() + theme(axis.text.x = element_text(angle = 90, hjust = 1)) + 
  labs(title = "Distribution of People Found per Keyword",
       y = "People_Found") 

ggsave("Distribution of People Found per Keyword.png", boxPlot_People_Keyword)

#---------------------
# Time Series Tests  -
#---------------------
# Time series for Crawling Time
timeSeriesCrawlingTime = data.frame(analytics$Date, analytics$Crawling_Duration)

timeSeriesCrawlingTimeAgg <-aggregate(timeSeriesCrawlingTime, 
                                 by=list(timeSeriesCrawlingTime$analytics.Date), 
                                 FUN=mean, na.rm=TRUE)

timeSeriesCrawlingTime.zoo <- with(timeSeriesCrawlingTimeAgg, 
                                   zoo(analytics.Crawling_Duration, order.by = analytics.Date))

timeSeriesCrawlingTime.ts <- as.ts(timeSeriesCrawlingTime.zoo)

# Time series for Added URIs
timeSeriesAddedURIs = data.frame(analytics$Date, analytics$Added_URIs)

timeSeriesAddedURIsAgg <-aggregate(timeSeriesAddedURIs, 
                                      by=list(timeSeriesAddedURIs$analytics.Date), 
                                      FUN=mean, na.rm=TRUE)

timeSeriesAddedURIs.zoo <- with(timeSeriesAddedURIsAgg, 
                                   zoo(analytics.Added_URIs, order.by = analytics.Date))

timeSeriesAddedURIs.ts <- as.ts(timeSeriesAddedURIs.zoo)


# Time series for People Found
timeSeriesPeopleFound = data.frame(analytics$Date, analytics$People_Found)

timeSeriesPeopleFoundAgg <-aggregate(timeSeriesPeopleFound, 
                                   by=list(timeSeriesPeopleFound$analytics.Date), 
                                   FUN=mean, na.rm=TRUE)

timeSeriesPeopleFound.zoo <- with(timeSeriesPeopleFoundAgg, 
                                zoo(analytics.People_Found, 
                                    order.by = analytics.Date))

timeSeriesPeopleFound.ts <- as.ts(timeSeriesPeopleFound.zoo)

# # Outlier plots
# outlier.analytics.Crawling <- tsoutliers::tso(timeSeriesCrawlingTime.ts, 
#                                      types = c("AO","LS","TC", "SLS"), maxit.iloop = 10,
#                                      remove.method = "bottom-up", tsmethod = "auto.arima", 
#                                      args.tsmethod = list(allowdrift = FALSE, ic = "bic"))
# 
# plot(outlier.analytics.Crawling)
# 
# outlier.analytics.AddedURIs <- tsoutliers::tso(timeSeriesAddedURIs.ts, 
#                                      types = c("AO","LS","TC", "SLS"), maxit.iloop = 10,
#                                      remove.method = "bottom-up", tsmethod = "auto.arima", 
#                                      args.tsmethod = list(allowdrift = FALSE, ic = "bic"))
# 
# plot(outlier.analytics.AddedURIs)
# 
# outlier.analytics.PeopleFound <- tsoutliers::tso(timeSeriesPeopleFound.ts, 
#                                               types = c("AO","LS","TC", "SLS"), maxit.iloop = 10,
#                                               remove.method = "bottom-up", tsmethod = "auto.arima", 
#                                               args.tsmethod = list(allowdrift = FALSE, ic = "bic"))
# 
# plot(outlier.analytics.PeopleFound)

#----------------------
# Correlation results -
#----------------------
library(ellipse)
library(xtable)
library(Hmisc)

sapply(analytics[!(names(analytics) %in% c("Date", "Keyword", "Branches_Factor", "From_Country"))], as.numeric)

corr_matrix_pearson <- cor(analytics[!(names(analytics) %in% 
                                         c("Date", "Keyword", "Branches_Factor", "From_Country"))])

colorfun <- colorRamp(c("#CC0000","white","#3366CC"), space="Lab")
plot_corr_pearson <- plotcorr(corr_matrix_pearson, col=rgb(colorfun((corr_matrix_pearson+1)/2), 
                                                           maxColorValue=255), mar = c(0.1, 0.1, 0.1, 0.1)) +
  title(xlab = "Pearson correlation matrix", outer = F)

heatmapGranger <- heatmap(corr_matrix_pearson, Colv = NA, Rowv = NA, scale='none', 
                          symm = F, margins=c(10,10), 
                          cex.main = 0.5, )

corstarsl <- function(x){ 
  require(Hmisc) 
  x <- as.matrix(x) 
  R <- rcorr(x)$r 
  p <- rcorr(x)$P 
  
  ## define notions for significance levels; spacing is important.
  mystars <- ifelse(p < .001, "***", ifelse(p < .01, "** ", ifelse(p < .05, "* ", " ")))
  
  ## trunctuate the matrix that holds the correlations to two decimal
  R <- format(round(cbind(rep(-1.11, ncol(x)), R), 2))[,-1] 
  
  ## build a new matrix that includes the correlations with their apropriate stars 
  Rnew <- matrix(paste(R, mystars, sep=""), ncol=ncol(x)) 
  diag(Rnew) <- paste(diag(R), " ", sep="") 
  rownames(Rnew) <- colnames(x) 
  colnames(Rnew) <- paste(colnames(x), "", sep="") 
  
  ## remove upper triangle
  Rnew <- as.matrix(Rnew)
  Rnew[upper.tri(Rnew, diag = TRUE)] <- ""
  Rnew <- as.data.frame(Rnew) 
  
  ## remove last column and return the matrix (which is now a data frame)
  Rnew <- cbind(Rnew[1:length(Rnew)-1])
  return(Rnew) 
}

xtable(corstarsl(analytics[!(names(analytics) %in% c("Date", "Keyword", "Branches_Factor", "From_Country"))]))

