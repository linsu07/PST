## overview
### sequence's postier probability,Markov Process
    we can define a sequence's posterior probability,like this:
    𝑃(s_1,𝑠_2…𝑠_𝑛 )=𝑃(𝑠_1 )∗𝑃(𝑠_2│𝑠_1 )∗𝑃(𝑠_3 |𝑠_1,𝑠_2 )…𝑃(𝑠_𝑛 |𝑠_1,𝑠_2,..𝑠_(𝑛−1) )
    we use markov process to simplify the above equation
    𝑃(𝑠_𝑛│𝑠_1 𝑠_(2..) 𝑠_(𝑛−1) )≈𝑃(𝑠_𝑛│𝑠_𝑚 𝑠_(𝑚+1..) 𝑠_(𝑛−1) )     1<𝑚<𝑛 
### PST
   Given a database of sequences, we do statistics on these sequences probabilities,But creating, storing, and efficiently searching these probabilities pose a significant challenge. To address this, the program utilizes Probability Suffix Trees (PST), which employ a tree-like architecture. The concepts and ideas behind PST can be found in the paper "Mining for Outliers in Sequential Databases.pdf" listed in the repository..

## features
   + The program extends the estimator interface and can seamlessly integrate with Spark ML lib in a pipeline.
   + It is designed to run in a distributed manner, taking advantage of the high-performance capabilities of Spark.
   + The final transformed results are the similarities between individual sequence and the PST tree

## Requirements
   * spark>=2.4
   * scala



