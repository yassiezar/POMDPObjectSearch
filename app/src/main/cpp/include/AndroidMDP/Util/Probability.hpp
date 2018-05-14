#ifndef ANDROIDMDP_UTIL_PROBABILITY_HPP
#define ANDROIDMDP_UTIL_PROBABILITY_HPP

#include <cstddef>
#include <random>
#include <algorithm>

#include <AndroidMDP/Types.hpp>
#include <AndroidMDP/Util/Core.hpp>

namespace AndroidMDP
{
    template <typename T>
    bool isProbability(const size_t d, const T &in)
    {
        double p = 0.0;
        for(size_t i = 0; i < d; i ++)
        {
            const double value = static_cast<double>(in[i]);
            if(value < 0.0) return false;
            p += value;
        }
        return !Util::checkDifferentSmall(p, 1.0);
    }

    template <typename G>
    size_t sampleProbability(const size_t d, const SparseMatrix2D::ConstRowXpr &in, G &generator)
    {
        static std::uniform_real_distribution<double> sampleDistribution(0.0, 1.0);
        double p = sampleDistribution(generator);

        for (SparseMatrix2D::ConstRowXpr::InnerIterator i(in, 0); ; ++i )
        {
            if (i.value() > p) return i.col();
            p -= i.value();
        }
        return d-1;
    }

    template <typename T, typename G>
    size_t sampleProbability(const size_t d, const T& in, G& generator) {
        static std::uniform_real_distribution<double> sampleDistribution(0.0, 1.0);
        double p = sampleDistribution(generator);

        for ( size_t i = 0; i < d; ++i ) {
            if ( in[i] > p ) return i;
            p -= in[i];
        }
        return d-1;
    }
}

#endif
