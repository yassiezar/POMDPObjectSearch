#ifndef ANDROIDMDP_POLICYINTERFACE_HPP
#define ANDROIDMDP_POLICYINTERFACE_HPP

#include <cstddef>
#include <random>
#include <iosfwd>

#include <AndroidMDP/Util/Seed.hpp>

namespace AndroidMDP
{
    template<typename State, typename Sampling, typename Action>
    class PolicyInterface
    {
    public:
        PolicyInterface(State s, Action a);

        virtual Action sampleAction(const Sampling &s) const = 0;

        const Action &getA() const;

     protected:
        State S;
        Action A;

        mutable std::default_random_engine rand_;
    };

    template <typename State, typename Sampling, typename Action>
    PolicyInterface<State, Sampling, Action>::PolicyInterface(State s, Action a) :
            S(std::move(s)), A(std::move(a)) { }

    template <typename State, typename Sampling, typename Action>
    const Action &PolicyInterface<State, Sampling, Action>::getA() const { return A; }
}

#endif