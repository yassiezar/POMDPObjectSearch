#ifndef ANDROIDMDP_MDP_TYPES_HPP
#define ANDROIDMDP_MDP_TYPES_HPP

#include <vector>

#include <AndroidMDP/Types.hpp>

namespace AndroidMDP
{
    namespace MDP
    {
        using Values = Vector;
        using Actions = std::vector<size_t>;

        template <typename M>
        struct is_generative_model
        {
        private:
            template<typename Z>
            static constexpr auto test(int) -> decltype(
                    static_cast<size_t (Z::*)() const> (&Z::getS),
                    static_cast<size_t (Z::*)() const> (&Z::getA),
                    static_cast<double (Z::*)() const> (&Z::getDiscount),
                    static_cast<std::tuple<size_t, double> (Z::*)(size_t, size_t) const> (&Z::sampleSR),
                    static_cast<bool (Z::*)(size_t) const> (&Z::isTerminal),
                    bool()
            )
            { return true; }

            template<typename>
            static constexpr auto test(...) -> bool
            { return false; }

        public:
            enum
            {
                value = test<M>(0)
            };
        };

        template <typename M>
        struct is_model
        {
        private:
            template <typename Z>
            static constexpr auto test(int) -> decltype(
                    static_cast<double (Z::*)(size_t, size_t, size_t) const> (&Z::getTransitionProbability),
                    static_cast<double (Z::*)(size_t, size_t, size_t) const> (&Z::getExpectedReward),
                    bool()
            )
            { return true; }

            template <typename>
            static constexpr auto test(...) -> bool
            { return false; }

        public:
            enum
            {
                value = test<M>(0) && is_generative_model<M>::value
            };
        };
    }
}

#endif
